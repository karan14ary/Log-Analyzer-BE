package com.coe.ailoganalyzer.loganalyzerv2.model;


import java.time.OffsetDateTime;
import java.util.Map;

public record LogEvent(

        OffsetDateTime timestamp,

        LogLevel level,

        LogFormat format,

        String service,

        String thread,

        String logger,

        String message,

        String traceId,

        String spanId,

        String correlationId,

        String rawLog,

        Map<String, String> metadata

) {
}