package com.coe.ailoganalyzer.loganalyzerv2.model;



public record DeploymentComparisonSummary(

        ComparisonStatus status,

        String message,

        int improvementCount,

        int regressionCount

) {
}