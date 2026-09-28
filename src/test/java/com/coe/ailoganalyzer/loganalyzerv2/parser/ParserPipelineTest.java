package com.coe.ailoganalyzer.loganalyzerv2.parser;

import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import com.coe.ailoganalyzer.loganalyzerv2.parser.strategies.GenericLogParser;
import com.coe.ailoganalyzer.loganalyzerv2.parser.strategies.JsonLogParser;
import com.coe.ailoganalyzer.loganalyzerv2.parser.strategies.LogParsingEngine;
import com.coe.ailoganalyzer.loganalyzerv2.parser.strategies.SpringBootLogParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserPipelineTest {

    private final LogEntrySplitter splitter = new LogEntrySplitter();
    private final GenericLogParser genericParser = new GenericLogParser();
    private final SpringBootLogParser springBootParser = new SpringBootLogParser();
    private final JsonLogParser jsonParser = new JsonLogParser(new ObjectMapper());

    @Test
    void splitsEntriesAndKeepsContinuationLines() {
        List<String> entries = splitter.split(
                "2025-01-01T00:00:00Z ERROR first\n  at app.First.run(First.java:1)\n\n"
                        + "{\"message\":\"second\"}\n{\"message\":\"third\"}"
        );

        assertEquals(3, entries.size());
        assertTrue(entries.getFirst().contains("at app.First.run"));
        assertEquals("{\"message\":\"second\"}", entries.get(1));
    }

    @Test
    void splitReturnsNoEntriesForBlankInput() {
        assertTrue(splitter.split(" \n\t\n").isEmpty());
    }

    @Test
    void genericParserExtractsTimestampLevelAndCorrelationIds() {
        var event = genericParser.parse(
                "2025-01-02T03:04:05Z ERROR request failed traceId=t-1 spanId=s-2 correlation-id=c-3"
        );

        assertEquals(OffsetDateTime.parse("2025-01-02T03:04:05Z"), event.timestamp());
        assertEquals(LogLevel.ERROR, event.level());
        assertEquals(LogFormat.GENERIC, event.format());
        assertEquals("t-1", event.traceId());
        assertEquals("s-2", event.spanId());
        assertEquals("c-3", event.correlationId());
        assertNull(event.service());
    }

    @Test
    void genericParserDetectsLevelsAndMissingTimestamp() {
        assertEquals(LogLevel.FATAL, genericParser.parse("fatal incident").level());
        assertEquals(LogLevel.WARN, genericParser.parse("warning occurred").level());
        assertEquals(LogLevel.DEBUG, genericParser.parse("debug details").level());
        assertEquals(LogLevel.TRACE, genericParser.parse("trace details").level());
        assertEquals(LogLevel.INFO, genericParser.parse("info details").level());
        assertEquals(LogLevel.UNKNOWN, genericParser.parse("plain message").level());
        assertNull(genericParser.parse("plain message").timestamp());
        assertTrue(genericParser.supports("anything"));
    }

    @Test
    void springBootParserHandlesOffsetAndLocalTimestamps() {
        String offsetLog = "2025-01-02T03:04:05Z ERROR 42 --- [worker] app.Logger : failed traceId=t1";
        String localLog = "2025-01-02 03:04:05.123 WARN 42 --- [worker] app.Logger : slow";

        assertTrue(springBootParser.supports(offsetLog));
        assertFalse(springBootParser.supports("not a spring log"));
        assertEquals(LogFormat.SPRING_BOOT, springBootParser.parse(offsetLog).format());
        assertEquals("worker", springBootParser.parse(offsetLog).thread());
        assertEquals("app.Logger", springBootParser.parse(offsetLog).logger());
        assertEquals("failed traceId=t1", springBootParser.parse(offsetLog).message());
        assertEquals("t1", springBootParser.parse(offsetLog).traceId());
        assertEquals(OffsetDateTime.parse("2025-01-02T03:04:05.123Z"),
                springBootParser.parse(localLog).timestamp());
        assertThrows(IllegalArgumentException.class,
                () -> springBootParser.parse("not a spring log"));
    }

    @Test
    void jsonParserReadsKnownFieldsAndPreservesAdditionalMetadata() {
        String line = "{\"timestamp\":\"2025-01-02T03:04:05Z\",\"level\":\"error\","
                + "\"service\":\"payments\",\"thread\":\"worker\",\"logger\":\"app.Log\","
                + "\"message\":\"failed\",\"traceId\":\"t1\",\"spanId\":\"s1\","
                + "\"correlationId\":\"c1\",\"attempt\":2}";

        assertTrue(jsonParser.supports(line));
        var event = jsonParser.parse(line);
        assertEquals(LogFormat.JSON, event.format());
        assertEquals(LogLevel.ERROR, event.level());
        assertEquals("payments", event.service());
        assertEquals("worker", event.thread());
        assertEquals("app.Log", event.logger());
        assertEquals("failed", event.message());
        assertEquals("t1", event.traceId());
        assertEquals("s1", event.spanId());
        assertEquals("c1", event.correlationId());
        assertEquals("2", event.metadata().get("attempt"));
    }

    @Test
    void jsonParserHandlesMissingAndInvalidFields() {
        assertFalse(jsonParser.supports("{invalid"));
        assertFalse(jsonParser.supports("plain text"));
        assertEquals(LogLevel.UNKNOWN, jsonParser.parse("{\"level\":\"other\"}").level());
        assertNull(jsonParser.parse("{}").timestamp());
        assertThrows(IllegalArgumentException.class,
                () -> jsonParser.parse("{\"timestamp\":\"bad\"}"));
    }

    @Test
    void parsingEngineUsesFirstSupportingParserAndFailsWithoutOne() {
        var engine = new LogParsingEngine(List.of(springBootParser, jsonParser, genericParser));
        assertEquals(LogFormat.JSON, engine.parse("{\"message\":\"ok\"}").format());

        var emptyEngine = new LogParsingEngine(List.of());
        assertThrows(IllegalStateException.class, () -> emptyEngine.parse("anything"));
    }
}
