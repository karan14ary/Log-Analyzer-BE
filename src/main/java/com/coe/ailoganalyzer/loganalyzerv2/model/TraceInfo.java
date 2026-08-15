package com.coe.ailoganalyzer.loganalyzerv2.model;


import java.util.List;

public record TraceInfo(

        String traceId,

        String correlationId,

        int eventCount,

        int errorCount,

        List<LogEventAnalysis> events

) {
}