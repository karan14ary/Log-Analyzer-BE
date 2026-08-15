package com.coe.ailoganalyzer.loganalyzerv2.model;


import java.util.List;

public record AnomalyDetectionResult(

        int totalEvents,

        int totalGroups,

        int totalTraces,

        int anomalyCount,

        List<Anomaly> anomalies

) {
}