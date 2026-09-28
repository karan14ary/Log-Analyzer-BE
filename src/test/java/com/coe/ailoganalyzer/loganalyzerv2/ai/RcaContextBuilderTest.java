package com.coe.ailoganalyzer.loganalyzerv2.ai;

import com.coe.ailoganalyzer.loganalyzerv2.model.Anomaly;
import com.coe.ailoganalyzer.loganalyzerv2.model.AnomalySeverity;
import com.coe.ailoganalyzer.loganalyzerv2.model.AnomalyType;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroup;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroupingResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.ExceptionInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import com.coe.ailoganalyzer.loganalyzerv2.model.TraceTimeline;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RcaContextBuilderTest {

    private final RcaContextBuilder builder = new RcaContextBuilder();

    @Test
    void includesErrorSamplesExceptionDetailsTimelinesAndAnomalies() {
        var withException = analysis("connection failed", new ExceptionInfo(
                "IllegalStateException", "connection failed", List.of(), List.of(),
                "IllegalStateException", "connection failed"));
        var withoutException = analysis("retry exhausted", null);
        var group = new ErrorGroup("fingerprint-1", "connection failed", 2,
                Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-01T00:01:00Z"),
                List.of(withException, withoutException));
        var grouping = new ErrorGroupingResult(2, 1, List.of(group));
        var timeline = new TraceTimeline("trace-1", "correlation-1", Instant.EPOCH,
                Instant.EPOCH.plusSeconds(2), Duration.ofSeconds(2), 2, 1,
                "fingerprint-1", List.of(withException));
        var anomaly = new Anomaly(AnomalyType.ERROR_BURST, AnomalySeverity.HIGH,
                "fingerprint-1", "trace-1", "burst detected", 2.5, Instant.EPOCH);

        String context = builder.build(grouping, List.of(timeline), List.of(anomaly));

        assertTrue(context.contains("Fingerprint: fingerprint-1"));
        assertTrue(context.contains("Occurrences: 2"));
        assertTrue(context.contains("- connection failed | Exception: IllegalStateException"));
        assertTrue(context.contains("- retry exhausted\n"));
        assertTrue(context.contains("Trace ID: trace-1"));
        assertTrue(context.contains("Root cause candidate: fingerprint-1"));
        assertTrue(context.contains("Type: ERROR_BURST, Severity: HIGH, Score: 2.5"));
        assertTrue(context.contains("Description: burst detected"));
    }

    @Test
    void emitsSectionsWhenNoEvidenceWasFound() {
        String context = builder.build(new ErrorGroupingResult(0, 0, List.of()), List.of(), List.of());

        assertTrue(context.contains("ERROR GROUPS:"));
        assertTrue(context.contains("TRACE TIMELINES:"));
        assertTrue(context.contains("ANOMALIES:"));
    }

    private static LogEventAnalysis analysis(String message, ExceptionInfo exception) {
        var event = new LogEvent(null, LogLevel.ERROR, LogFormat.GENERIC, null, null,
                null, message, null, null, null, message, Map.of());
        return new LogEventAnalysis(event, exception, null, null, null, null);
    }
}
