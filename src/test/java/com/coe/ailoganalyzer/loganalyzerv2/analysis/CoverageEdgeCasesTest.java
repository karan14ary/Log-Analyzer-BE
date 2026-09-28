package com.coe.ailoganalyzer.loganalyzerv2.analysis;

import com.coe.ailoganalyzer.loganalyzerv2.anomaly.AnomalyDetectionService;
import com.coe.ailoganalyzer.loganalyzerv2.config.CorsConfig;
import com.coe.ailoganalyzer.loganalyzerv2.config.JacksonConfig;
import com.coe.ailoganalyzer.loganalyzerv2.grouping.ErrorGroupingService;
import com.coe.ailoganalyzer.loganalyzerv2.model.*;
import com.coe.ailoganalyzer.loganalyzerv2.severity.SeverityAnalysisService;
import com.coe.ailoganalyzer.loganalyzerv2.severity.SeverityDetector;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import com.coe.ailoganalyzer.loganalyzerv2.trace.TraceTimelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CoverageEdgeCasesTest {

    @Test
    void groupingUsesCurrentTimeForMissingEventTimestamps() {
        var event = event(null, LogLevel.ERROR, "message");
        var analysis = analysis(event, "trace", "fp");

        var result = new ErrorGroupingService().group(List.of(analysis));

        assertEquals(1, result.totalEvents());
        assertNotNull(result.groups().getFirst().firstOccurrence());
        assertNotNull(result.groups().getFirst().lastOccurrence());
    }

    @Test
    void severityAnalysisRebuildsGroupsAndHandlesEmptyResults() {
        var event = event(OffsetDateTime.parse("2025-01-01T00:00:00Z"), LogLevel.ERROR, "failed");
        var sample = new LogEventAnalysis(event, null, null, new FingerprintInfo("fp", "failed"), null, null);
        var group = new ErrorGroup("fp", "failed", 1, Instant.EPOCH, Instant.EPOCH, List.of(sample));
        var service = new SeverityAnalysisService(new SeverityDetector());

        ErrorGroupingResult analyzed = service.analyze(new ErrorGroupingResult(1, 1, List.of(group)));

        assertEquals(1, analyzed.groups().size());
        assertEquals(group.fingerprint(), analyzed.groups().getFirst().fingerprint());
        assertTrue(service.analyze(new ErrorGroupingResult(0, 0, List.of())).groups().isEmpty());
    }

    @Test
    void anomalyDetectionSkipsThresholdsAndReportsSingletons() {
        var service = new AnomalyDetectionService();
        var info = analysis(event(OffsetDateTime.parse("2025-01-01T00:00:00Z"), LogLevel.INFO, "ok"), null, "fp");
        var singleton = new ErrorGroup("fp", "ok", 1, Instant.EPOCH, Instant.EPOCH, List.of(info));
        var shortTrace = new TraceTimeline("trace", null, Instant.EPOCH, Instant.EPOCH,
                Duration.ofSeconds(5), 1, 0, null, List.of(info));

        var result = service.detect(List.of(info), new ErrorGroupingResult(1, 1, List.of(singleton)), List.of(shortTrace));

        assertEquals(1, result.anomalyCount());
        assertEquals(AnomalyType.NEW_FINGERPRINT, result.anomalies().getFirst().type());
        assertEquals(0, service.detect(List.of(), new ErrorGroupingResult(0, 0, List.of()), List.of())
                .anomalyCount());
    }

    @Test
    void traceTimelineHandlesInfoOnlyAndTimestampLessEvents() {
        var service = new TraceTimelineService();
        var info = analysis(event(OffsetDateTime.parse("2025-01-01T00:00:00Z"), LogLevel.INFO, "ok"),
                "trace-info", "fp-info");
        var untimed = analysis(event(null, LogLevel.ERROR, "bad"), "trace-untimed", "fp-untimed");

        List<TraceTimeline> timelines = service.buildTimelines(List.of(info, untimed));

        assertEquals(2, timelines.size());
        assertEquals(0, timelines.getFirst().errorCount());
        assertEquals(1, timelines.getLast().errorCount());
        assertEquals("fp-untimed", timelines.getLast().events().getFirst().fingerprintInfo().fingerprint());
    }

    @Test
    void deploymentComparisonReportsIncreasedFingerprintAndAnomalyChanges() {
        var service = new com.coe.ailoganalyzer.loganalyzerv2.comparision.DeploymentComparisonService();
        var beforeAnalyses = List.of(
                analysis(event(OffsetDateTime.parse("2025-01-01T00:00:00Z"), LogLevel.ERROR, "same"), null, "same"),
                analysis(event(OffsetDateTime.parse("2025-01-01T00:00:01Z"), LogLevel.ERROR, "same"), null, "same"),
                analysis(event(OffsetDateTime.parse("2025-01-01T00:00:02Z"), LogLevel.INFO, "ok"), null, "same"),
                analysis(event(OffsetDateTime.parse("2025-01-01T00:00:03Z"), LogLevel.INFO, "ok"), null, "same"));
        var afterAnalyses = List.of(
                analysis(event(OffsetDateTime.parse("2025-01-01T00:00:00Z"), LogLevel.ERROR, "same"), null, "same"),
                analysis(event(OffsetDateTime.parse("2025-01-01T00:00:01Z"), LogLevel.ERROR, "same"), null, "same"));
        var before = context(beforeAnalyses, List.of(new ErrorGroup("same", "same", 2,
                Instant.EPOCH, Instant.EPOCH, List.of())), List.of(anomaly(AnomalyType.SLOW_TRACE)));
        var after = context(afterAnalyses, List.of(new ErrorGroup("same", "same", 4,
                Instant.EPOCH, Instant.EPOCH, List.of())), List.of(anomaly(AnomalyType.ERROR_BURST)));

        var result = service.compare(before, after);

        assertEquals(ComparisonStatus.MIXED, result.summary().status());
        assertTrue(result.errors().increasedErrors().contains("same"));
        assertTrue(result.anomalies().newAnomalies().contains("ERROR_BURST"));
        assertTrue(result.anomalies().resolvedAnomalies().contains("SLOW_TRACE"));
        assertTrue(result.recommendations().stream().anyMatch(text -> text.contains("frequency increased")));
        assertTrue(result.recommendations().stream().anyMatch(text -> text.contains("error rate increased")));
    }

    @Test
    void recommendationDeserializerFlattensNestedContent() throws Exception {
        RcaResult result = new ObjectMapper().readValue(
                "{\"rootCause\":\"x\",\"confidence\":0.5,\"impact\":\"y\",\"evidence\":[],"
                        + "\"recommendations\":[[\"a\",\"b\"],{\"step\":\"c\"},null]}", RcaResult.class);

        assertEquals(java.util.Arrays.asList("a; b", "c", null), result.recommendations());
    }

    @Test
    void corsAndJacksonConfigurationsCanBeInstantiated() {
        var corsConfigurer = ReflectionTestUtils.invokeMethod(new CorsConfig(), "corsConfigurer");
        ((WebMvcConfigurer) corsConfigurer).addCorsMappings(new CorsRegistry());
        assertNotNull(new JacksonConfig().objectMapper());
        assertEquals(0, new DeploymentComparision().getClass().getRecordComponents().length);
    }

    @Test
    void ollamaHealthServiceReturnsTrueForSuccessfulRootResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var service = new com.coe.ailoganalyzer.loganalyzerv2.ai.OllamaHealthService(builder,
                new com.coe.ailoganalyzer.loganalyzerv2.ai.OllamaConfig("http://localhost:11434", "model", 0.1));
        server.expect(requestTo("http://localhost:11434/")).andRespond(withSuccess());

        assertTrue(service.isAvailable());
        server.verify();
    }

    private static Anomaly anomaly(AnomalyType type) {
        return new Anomaly(type, AnomalySeverity.LOW, "fp", null, "description", 1, Instant.EPOCH);
    }

    private static LogEventAnalysis analysis(LogEvent event, String traceId, String fingerprint) {
        return new LogEventAnalysis(event, null, null, new FingerprintInfo(fingerprint, event.message()), traceId, null);
    }

    private static LogEvent event(OffsetDateTime timestamp, LogLevel level, String message) {
        return new LogEvent(timestamp, level, LogFormat.GENERIC, null, null, null, message,
                null, null, null, message, Map.of());
    }

    private static LogAnalysisContext context(List<LogEventAnalysis> analyses, List<ErrorGroup> groups,
                                              List<Anomaly> anomalies) {
        return new LogAnalysisContext(analyses, new ErrorGroupingResult(analyses.size(), groups.size(), groups),
                List.of(), anomalies);
    }
}
