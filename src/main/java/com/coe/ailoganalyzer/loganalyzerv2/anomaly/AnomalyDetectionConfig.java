package com.coe.ailoganalyzer.loganalyzerv2.anomaly;


import java.time.Duration;

public record AnomalyDetectionConfig(

        int errorBurstThreshold,

        Duration errorBurstWindow,

        int highFrequencyThreshold,

        Duration highFrequencyWindow,

        Duration slowTraceThreshold

) {

    public static AnomalyDetectionConfig defaults() {

        return new AnomalyDetectionConfig(
                20,
                Duration.ofMinutes(1),
                100,
                Duration.ofMinutes(5),
                Duration.ofSeconds(5)
        );
    }
}