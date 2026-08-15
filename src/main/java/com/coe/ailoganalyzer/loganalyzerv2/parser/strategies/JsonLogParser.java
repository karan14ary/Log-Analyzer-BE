package com.coe.ailoganalyzer.loganalyzerv2.parser.strategies;


import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import com.coe.ailoganalyzer.loganalyzerv2.parser.LogParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class JsonLogParser implements LogParser {

    private final ObjectMapper objectMapper;

    public JsonLogParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String logEntry) {

        String value = logEntry.trim();

        if (!value.startsWith("{")) {
            return false;
        }

        try {
            objectMapper.readTree(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public LogEvent parse(String logEntry) {

        try {

            JsonNode json =
                    objectMapper.readTree(
                            logEntry.trim()
                    );

            return new LogEvent(
                    getTimestamp(json),
                    getLevel(json),
                    LogFormat.JSON,
                    getText(json, "service"),
                    getText(json, "thread"),
                    getText(json, "logger"),
                    getText(json, "message"),
                    getText(json, "traceId"),
                    getText(json, "spanId"),
                    getText(json, "correlationId"),
                    logEntry,
                    extractMetadata(json)
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Unable to parse JSON log",
                    e
            );
        }
    }

    private OffsetDateTime getTimestamp(
            JsonNode json) {

        String value =
                getText(json, "timestamp");

        if (value == null) {
            return null;
        }

        return OffsetDateTime.parse(value);
    }

    private LogLevel getLevel(JsonNode json) {

        String level =
                getText(json, "level");

        if (level == null) {
            return LogLevel.UNKNOWN;
        }

        try {
            return LogLevel.valueOf(
                    level.toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            return LogLevel.UNKNOWN;
        }
    }

    private String getText(
            JsonNode json,
            String field) {

        JsonNode value =
                json.get(field);

        return value == null || value.isNull()
                ? null
                : value.asText();
    }

    private Map<String, String> extractMetadata(
            JsonNode json) {

        Map<String, String> metadata =
                new HashMap<>();

        json.fields().forEachRemaining(entry -> {

            String field = entry.getKey();

            if (!isKnownField(field)) {

                metadata.put(
                        field,
                        entry.getValue().asText()
                );
            }
        });

        return metadata;
    }

    private boolean isKnownField(String field) {

        return switch (field) {

            case "timestamp",
                 "level",
                 "service",
                 "thread",
                 "logger",
                 "message",
                 "traceId",
                 "spanId",
                 "correlationId" -> true;

            default -> false;
        };
    }
}