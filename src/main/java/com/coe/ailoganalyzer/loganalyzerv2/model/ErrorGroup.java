package com.coe.ailoganalyzer.loganalyzerv2.model;


import java.time.Instant;
import java.util.List;




public record ErrorGroup(

        String fingerprint,

        String normalizedMessage,

        int occurrenceCount,

        Instant firstOccurrence,

        Instant lastOccurrence,

        List<LogEventAnalysis> samples

) {
}