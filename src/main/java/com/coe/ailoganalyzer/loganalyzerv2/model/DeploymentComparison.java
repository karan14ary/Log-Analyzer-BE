package com.coe.ailoganalyzer.loganalyzerv2.model;

import java.util.List;

public record DeploymentComparison(

        DeploymentComparisonSummary summary,

        MetricComparison metrics,

        ErrorComparison errors,

        AnomalyComparison anomalies,

        List<String> recommendations

) {
}