package com.coe.ailoganalyzer.loganalyzerv2.model;

import java.time.Instant;

public record Anomaly(

        AnomalyType type,

        AnomalySeverity severity,

        String fingerprint,

        String traceId,

        String description,

        double score,

        Instant detectedAt

) {
}