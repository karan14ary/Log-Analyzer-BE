package com.coe.ailoganalyzer.loganalyzerv2.model;


import java.util.List;

public record RcaResult(

        String rootCause,

        double confidence,

        String impact,

        List<String> evidence,

        List<String> recommendations

) {
}