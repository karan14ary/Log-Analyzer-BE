package com.coe.ailoganalyzer.loganalyzerv2.report;

import com.coe.ailoganalyzer.loganalyzerv2.model.Anomaly;
import com.coe.ailoganalyzer.loganalyzerv2.model.AnomalySeverity;
import com.coe.ailoganalyzer.loganalyzerv2.model.AnomalyType;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroup;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroupingResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.FingerprintInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.IncidentReport;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import com.coe.ailoganalyzer.loganalyzerv2.model.RcaAnalysisResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.RcaResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.TraceTimeline;
import com.coe.ailoganalyzer.loganalyzerv2.report.IncidentMarkdownRenderer;
import com.coe.ailoganalyzer.loganalyzerv2.report.IncidentReportService;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentReportTest {

    private final IncidentReportService service = new IncidentReportService();
    private final IncidentMarkdownRenderer renderer = new IncidentMarkdownRenderer();

    @Test
    void reportAggregatesCountsTimesSeverityAndSuccessfulRca() {
        var first = analysis("2025-01-01T00:00:00Z", LogLevel.ERROR);
        var second = analysis("2025-01-01T00:02:00Z", LogLevel.FATAL);
        var grouping = new ErrorGroupingResult(2, 1, List.of(
                new ErrorGroup("fp-1", "failed", 2, Instant.EPOCH, Instant.EPOCH, List.of(first))));
        var timeline = new TraceTimeline("t-1", "c-1", Instant.EPOCH, Instant.EPOCH,
                Duration.ofSeconds(5), 2, 1, "fp-1", List.of(first));
        var anomaly = new Anomaly(AnomalyType.ERROR_BURST, AnomalySeverity.HIGH,
                "fp-1", "t-1", "error rate increased", 0.9, Instant.EPOCH);
        var rca = new RcaAnalysisResult(true,
                new RcaResult("database pool exhausted", 0.95, "requests failed",
                        List.of("pool timeout"), List.of("increase pool capacity")), null);
        var context = new LogAnalysisContext(List.of(first, second), grouping,
                List.of(timeline), List.of(anomaly));

        IncidentReport report = service.generate(context, rca);

        assertTrue(report.incidentId().matches("INC-[A-F0-9]{8}"));
        assertEquals("HIGH Incident - database pool exhausted", report.title());
        assertEquals(AnomalySeverity.HIGH, report.severity());
        assertEquals("database pool exhausted. requests failed", report.summary());
        assertEquals(2, report.totalEvents());
        assertEquals(2, report.totalErrors());
        assertEquals(1, report.totalTraces());
        assertEquals(1, report.affectedTraces());
        assertEquals(Duration.ofMinutes(2), report.duration());
        assertEquals(List.of("fp-1"), report.fingerprints());
        assertEquals(List.of("increase pool capacity"), report.recommendations());
    }

    @Test
    void reportUsesFallbackSummaryAndEmptyRecommendationsForFailedRca() {
        var warning = analysis("2025-01-01T00:00:00Z", LogLevel.WARN);
        var grouping = new ErrorGroupingResult(1, 1, List.of());
        var timelines = List.of(
                new TraceTimeline("t1", null, Instant.EPOCH, Instant.EPOCH, Duration.ZERO,
                        1, 0, null, List.of()),
                new TraceTimeline("t2", null, Instant.EPOCH, Instant.EPOCH, Duration.ZERO,
                        1, 1, null, List.of()));

        IncidentReport report = service.generate(List.of(warning), grouping, timelines,
                List.of(), new RcaAnalysisResult(false, null, "model error"));

        assertEquals("LOW Application Incident", report.title());
        assertEquals("Detected 0 errors affecting 1 traces with 0 anomalies.", report.summary());
        assertEquals(0, report.totalErrors());
        assertEquals(1, report.affectedTraces());
        assertTrue(report.recommendations().isEmpty());
        assertEquals(AnomalySeverity.LOW, report.severity());
    }

    @Test
    void markdownIncludesSuccessfulRcaEvidenceAnomaliesAndRecommendations() {
        IncidentReport report = new IncidentReport("INC-12345678", "HIGH Incident", AnomalySeverity.HIGH,
                "Summary", Instant.EPOCH, Instant.EPOCH, Duration.ZERO, 4, 2, 1, 1,
                List.of("fp"), List.of(new Anomaly(AnomalyType.SLOW_TRACE, AnomalySeverity.MEDIUM,
                "fp", "t1", "trace is slow", 0.7, Instant.EPOCH)),
                new RcaAnalysisResult(true, new RcaResult("slow database", 0.8, "impact",
                        List.of("query took 4s"), List.of("add an index")), null), List.of("add an index"));

        String markdown = renderer.render(report);

        assertTrue(markdown.contains("# HIGH Incident"));
        assertTrue(markdown.contains("**slow database**"));
        assertTrue(markdown.contains("- query took 4s"));
        assertTrue(markdown.contains("trace is slow"));
        assertTrue(markdown.contains("- add an index"));
    }

    @Test
    void markdownOmitsRcaDetailsWhenAnalysisFailed() {
        IncidentReport report = new IncidentReport("INC-12345678", "LOW Incident", AnomalySeverity.LOW,
                "Summary", Instant.EPOCH, Instant.EPOCH, Duration.ZERO, 0, 0, 0, 0,
                List.of(), List.of(), new RcaAnalysisResult(false, null, "failed"), List.of());

        String markdown = renderer.render(report);

        assertTrue(markdown.contains("## Root Cause"));
        assertFalse(markdown.contains("### Evidence"));
        assertTrue(markdown.contains("## Recommendations"));
    }

    @Test
    void rcaResultIgnoresUnexpectedModelFields() throws Exception {
        RcaResult result = new ObjectMapper().readValue(
                "{\"type\":\"analysis\",\"rootCause\":\"test\",\"confidence\":0.8,"
                        + "\"impact\":\"minor\",\"evidence\":[],\"recommendations\":[]}",
                RcaResult.class);

        assertEquals("test", result.rootCause());
        assertEquals(0.8, result.confidence());
    }

        @Test
        void rcaResultAcceptsObjectRecommendations() throws Exception {
                RcaResult result = new ObjectMapper().readValue(
                                "{\"rootCause\":\"test\",\"confidence\":0.8,\"impact\":\"minor\","
                                                + "\"evidence\":[],\"recommendations\":[{\"action\":\"increase pool capacity\","
                                                + "\"reason\":\"requests are timing out\"}]}",
                                RcaResult.class);

                assertEquals(List.of("increase pool capacity; requests are timing out"), result.recommendations());
        }

    private static LogEventAnalysis analysis(String timestamp, LogLevel level) {
        var event = new LogEvent(OffsetDateTime.parse(timestamp), level, LogFormat.GENERIC,
                "service", null, null, "failed", null, null, null, "failed", Map.of());
        return new LogEventAnalysis(event, null, null, new FingerprintInfo("fp-1", "failed"), null, null);
    }
}
