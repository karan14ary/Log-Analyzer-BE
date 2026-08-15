package com.coe.ailoganalyzer.loganalyzerv2.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record TraceTimeline(

        String traceId,

        String correlationId,

        Instant startTime,

        Instant endTime,

        Duration duration,

        int eventCount,

        int errorCount,

        String rootCauseFingerprint,

        List<LogEventAnalysis> events



) {
}