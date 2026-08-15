package com.coe.ailoganalyzer.loganalyzerv2.model;


import java.util.List;

public record ErrorGroupingResult(

        int totalEvents,

        int uniqueFingerprints,

        List<ErrorGroup> groups

) {
}