package com.coe.ailoganalyzer.loganalyzerv2.model;


import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record IncidentReport(

        String incidentId,

        String title,

        AnomalySeverity severity,

        String summary,

        Instant startTime,

        Instant endTime,

        Duration duration,

        int totalEvents,

        int totalErrors,

        int totalTraces,

        int affectedTraces,

        List<String> fingerprints,

        List<Anomaly> anomalies,

        RcaAnalysisResult rca,

        List<String> recommendations

) {
}