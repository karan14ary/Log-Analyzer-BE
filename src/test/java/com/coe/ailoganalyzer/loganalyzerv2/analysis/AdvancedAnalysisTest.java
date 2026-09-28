package com.coe.ailoganalyzer.loganalyzerv2.analysis;

import com.coe.ailoganalyzer.loganalyzerv2.anomaly.AnomalyDetectionService;
import com.coe.ailoganalyzer.loganalyzerv2.comparision.DeploymentComparisonService;
import com.coe.ailoganalyzer.loganalyzerv2.model.Anomaly;
import com.coe.ailoganalyzer.loganalyzerv2.model.AnomalySeverity;
import com.coe.ailoganalyzer.loganalyzerv2.model.AnomalyType;
import com.coe.ailoganalyzer.loganalyzerv2.model.ComparisonStatus;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroup;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroupingResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.FingerprintInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import com.coe.ailoganalyzer.loganalyzerv2.model.TraceTimeline;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import com.coe.ailoganalyzer.loganalyzerv2.trace.TraceIdExtractor;
import com.coe.ailoganalyzer.loganalyzerv2.trace.TraceTimelineService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedAnalysisTest {

    @Test
    void extractsTraceAndCorrelationIdsCaseInsensitively() {
        var extractor = new TraceIdExtractor();
        assertEquals("abc-1", extractor.extractTraceId("TRACE_ID=abc-1"));
        assertEquals("xyz", extractor.extractCorrelationId("correlation-id: xyz"));
        assertNull(extractor.extractTraceId(null));
        assertNull(extractor.extractCorrelationId("  "));
        assertNull(extractor.extractTraceId("no ids here"));
    }

    @Test
    void timelinesSortEventsAndHandleMissingTimestamps() {
        var service = new TraceTimelineService();
        var late = analysis("trace-1", "corr", "2025-01-01T00:00:03Z", LogLevel.ERROR, "fp-error");
        var early = analysis("trace-1", null, "2025-01-01T00:00:01Z", LogLevel.INFO, "fp-info");
        var missing = analysis("trace-2", "corr-2", null, LogLevel.ERROR, "fp-missing");
        var untraced = analysis(null, null, "2025-01-01T00:00:00Z", LogLevel.INFO, "fp-no-trace");

        List<TraceTimeline> timelines = service.buildTimelines(List.of(late, early, missing, untraced));

        assertEquals(2, timelines.size());
        assertEquals("trace-1", timelines.getFirst().traceId());
        assertEquals(2, timelines.getFirst().eventCount());
        assertEquals("corr", timelines.getFirst().correlationId());
        assertEquals("fp-error", timelines.getFirst().rootCauseFingerprint());
        assertEquals(Duration.ofSeconds(2), timelines.getFirst().duration());
        assertEquals(1, timelines.getLast().errorCount());
        assertEquals(Duration.ZERO, timelines.getLast().duration());
        assertTrue(service.buildTimelines(List.of()).isEmpty());
    }

    @Test
    void anomalyDetectionFindsFrequencyBurstsSlowTracesAndSingletonGroups() {
        var service = new AnomalyDetectionService();
        List<LogEventAnalysis> analyses = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            analyses.add(analysis("trace", null, "2025-01-01T00:00:00Z", LogLevel.ERROR, "frequent"));
        }
        var singleton = new ErrorGroup("new", "new error", 1, Instant.EPOCH, Instant.EPOCH, List.of());
        var grouping = new ErrorGroupingResult(100, 2, List.of(
                new ErrorGroup("frequent", "repeated", 100, Instant.EPOCH, Instant.EPOCH, List.of()), singleton));
        var timeline = new TraceTimeline("trace", null, Instant.EPOCH, Instant.EPOCH,
                Duration.ofSeconds(10), 100, 100, "frequent", List.of());

        var result = service.detect(analyses, grouping, List.of(timeline));

        assertEquals(100, result.totalEvents());
        assertEquals(1, result.totalTraces());
        assertEquals(4, result.anomalyCount());
        assertTrue(result.anomalies().stream().anyMatch(a -> a.type() == AnomalyType.HIGH_FREQUENCY));
        assertTrue(result.anomalies().stream().anyMatch(a -> a.type() == AnomalyType.ERROR_BURST
                && a.severity() == AnomalySeverity.HIGH));
        assertTrue(result.anomalies().stream().anyMatch(a -> a.type() == AnomalyType.SLOW_TRACE
                && a.severity() == AnomalySeverity.MEDIUM));
        assertTrue(result.anomalies().stream().anyMatch(a -> a.type() == AnomalyType.NEW_FINGERPRINT));
    }

    @Test
    void deploymentComparisonReportsImprovementRegressionMixedAndUnchanged() {
        var service = new DeploymentComparisonService();
        var unchanged = context(List.of(), List.of(), List.of());
        assertEquals(ComparisonStatus.UNCHANGED, service.compare(unchanged, unchanged).summary().status());

        var before = context(List.of(
                analysis(null, null, "2025-01-01T00:00:00Z", LogLevel.ERROR, "same"),
                analysis(null, null, "2025-01-01T00:00:01Z", LogLevel.ERROR, "same"),
                analysis(null, null, "2025-01-01T00:00:02Z", LogLevel.ERROR, "same")),
                List.of(group("same", 3)), List.of());
        var improved = context(List.of(
                analysis(null, null, "2025-01-01T00:00:00Z", LogLevel.ERROR, "same"),
                analysis(null, null, "2025-01-01T00:00:01Z", LogLevel.INFO, "same")),
                List.of(group("same", 1)), List.of());
        var improvementResult = service.compare(before, improved);
        assertEquals(ComparisonStatus.IMPROVED, improvementResult.summary().status());
        assertTrue(improvementResult.recommendations().size() > 0);

        var regressed = context(List.of(
                analysis(null, null, "2025-01-01T00:00:00Z", LogLevel.ERROR, "same"),
                analysis(null, null, "2025-01-01T00:00:01Z", LogLevel.ERROR, "new")),
                List.of(group("same", 1), group("new", 1)), List.of());
        var regressionResult = service.compare(improved, regressed);
        assertEquals(ComparisonStatus.REGRESSED, regressionResult.summary().status());
        assertTrue(regressionResult.recommendations().stream().anyMatch(text -> text.contains("newly introduced")));

        var mixed = context(List.of(
                analysis(null, null, "2025-01-01T00:00:00Z", LogLevel.ERROR, "new")),
                List.of(group("new", 1)), List.of());
        assertEquals(ComparisonStatus.MIXED, service.compare(before, mixed).summary().status());
    }

    private static LogEventAnalysis analysis(String traceId, String correlationId, String timestamp,
                                            LogLevel level, String fingerprint) {
        var event = new LogEvent(timestamp == null ? null : OffsetDateTime.parse(timestamp), level,
                LogFormat.GENERIC, null, null, null, fingerprint, null, null, null, fingerprint, Map.of());
        return new LogEventAnalysis(event, null, null,
                new FingerprintInfo(fingerprint, fingerprint), traceId, correlationId);
    }

    private static ErrorGroup group(String fingerprint, int count) {
        return new ErrorGroup(fingerprint, fingerprint, count, Instant.EPOCH, Instant.EPOCH, List.of());
    }

    private static LogAnalysisContext context(List<LogEventAnalysis> analyses, List<ErrorGroup> groups,
                                              List<Anomaly> anomalies) {
        var timelines = List.of(new TraceTimeline("trace", null, Instant.EPOCH, Instant.EPOCH,
                Duration.ZERO, 1, 0, null, List.of()));
        return new LogAnalysisContext(analyses,
                new ErrorGroupingResult(analyses.size(), groups.size(), groups), timelines, anomalies);
    }
}
