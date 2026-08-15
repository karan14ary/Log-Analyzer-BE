package com.coe.ailoganalyzer.loganalyzerv2.model;


public record MetricComparison(

        int beforeEvents,

        int afterEvents,

        int eventDifference,

        int beforeErrors,

        int afterErrors,

        int errorDifference,

        double beforeErrorRate,

        double afterErrorRate,

        double errorRateDifference,

        int beforeTraces,

        int afterTraces,

        int affectedTracesBefore,

        int affectedTracesAfter

) {
}