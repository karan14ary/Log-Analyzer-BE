package com.coe.ailoganalyzer.loganalyzerv2.model;

import java.util.List;

public record AnomalyComparison(

        List<String> newAnomalies,

        List<String> resolvedAnomalies,

        List<String> persistentAnomalies

) {
}