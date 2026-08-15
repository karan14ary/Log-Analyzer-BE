package com.coe.ailoganalyzer.loganalyzerv2.model;


public record RcaAnalysisResult(

        boolean successful,

        RcaResult result,

        String error

) {
}