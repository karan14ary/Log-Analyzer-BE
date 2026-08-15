package com.coe.ailoganalyzer.loganalyzerv2.model;


public record LogEventAnalysis(

        LogEvent event,

        ExceptionInfo exceptionInfo,

        MaskingResult maskingResult,

        FingerprintInfo fingerprintInfo,

        String traceId,

        String correlationId

) {
}