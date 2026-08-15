package com.coe.ailoganalyzer.loganalyzerv2.anomaly;

import com.coe.ailoganalyzer.loganalyzerv2.model.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnomalyDetectionService {

    private final AnomalyDetectionConfig config;

    public AnomalyDetectionService() {

        this.config =
                AnomalyDetectionConfig.defaults();
    }

    public AnomalyDetectionResult detect(
            List<LogEventAnalysis> analyses,
            ErrorGroupingResult groupingResult,
            List<TraceTimeline> timelines) {

        List<Anomaly> anomalies =
                new ArrayList<>();

        anomalies.addAll(
                detectHighFrequency(analyses)
        );

        anomalies.addAll(
                detectErrorBursts(analyses)
        );

        anomalies.addAll(
                detectSlowTraces(timelines)
        );

        anomalies.addAll(
                detectNewFingerprints(
                        analyses,
                        groupingResult
                )
        );

        return new AnomalyDetectionResult(
                analyses.size(),
                groupingResult.uniqueFingerprints(),
                timelines.size(),
                anomalies.size(),
                anomalies
        );
    }

    private List<Anomaly> detectHighFrequency(
            List<LogEventAnalysis> analyses) {

        List<Anomaly> anomalies =
                new ArrayList<>();

        Map<String, List<LogEventAnalysis>> byFingerprint =
                new HashMap<>();

        for (LogEventAnalysis analysis : analyses) {

            byFingerprint
                    .computeIfAbsent(
                            analysis.fingerprintInfo()
                                    .fingerprint(),
                            ignored ->
                                    new ArrayList<>()
                    )
                    .add(analysis);
        }

        for (Map.Entry<String, List<LogEventAnalysis>> entry
                : byFingerprint.entrySet()) {

            List<LogEventAnalysis> events =
                    entry.getValue();

            if (events.size()
                    < config.highFrequencyThreshold()) {

                continue;
            }

            Instant first =
                    events.stream()
                            .map(this::timestamp)
                            .min(Comparator.naturalOrder())
                            .orElse(null);

            Instant last =
                    events.stream()
                            .map(this::timestamp)
                            .max(Comparator.naturalOrder())
                            .orElse(null);

            if (first == null || last == null) {
                continue;
            }

            Duration duration =
                    Duration.between(
                            first,
                            last
                    );

            if (duration.compareTo(
                    config.highFrequencyWindow()
            ) <= 0) {

                double score =
                        (double) events.size()
                                / config.highFrequencyThreshold();

                anomalies.add(
                        new Anomaly(
                                AnomalyType.HIGH_FREQUENCY,
                                calculateSeverity(score),
                                entry.getKey(),
                                null,
                                "Fingerprint occurred "
                                        + events.size()
                                        + " times within "
                                        + duration,
                                score,
                                last
                        )
                );
            }
        }

        return anomalies;
    }

    private List<Anomaly> detectErrorBursts(
            List<LogEventAnalysis> analyses) {

        List<Anomaly> anomalies =
                new ArrayList<>();

        List<LogEventAnalysis> errors =
                analyses.stream()
                        .filter(this::isError)
                        .sorted(
                                Comparator.comparing(
                                        this::timestamp
                                )
                        )
                        .toList();

        int i = 0;

        while (i < errors.size()) {

            Instant start =
                    timestamp(errors.get(i));

            int j = i;

            while (j < errors.size()
                    && Duration.between(
                    start,
                    timestamp(errors.get(j))
            ).compareTo(
                    config.errorBurstWindow()
            ) <= 0) {

                j++;
            }

            int count = j - i;

            if (count >=
                    config.errorBurstThreshold()) {

                double score =
                        (double) count
                                / config.errorBurstThreshold();

                LogEventAnalysis first =
                        errors.get(i);

                anomalies.add(
                        new Anomaly(
                                AnomalyType.ERROR_BURST,
                                calculateSeverity(score),
                                first.fingerprintInfo()
                                        .fingerprint(),
                                first.traceId(),
                                "Detected "
                                        + count
                                        + " errors within "
                                        + config.errorBurstWindow(),
                                score,
                                start
                        )
                );

                /*
                 * Skip this burst so we don't report
                 * essentially the same anomaly repeatedly.
                 */
                i = j;

            } else {

                i++;
            }
        }

        return anomalies;
    }

    private List<Anomaly> detectSlowTraces(
            List<TraceTimeline> timelines) {

        List<Anomaly> anomalies =
                new ArrayList<>();

        for (TraceTimeline timeline : timelines) {

            if (timeline.duration()
                    .compareTo(
                            config.slowTraceThreshold()
                    ) <= 0) {

                continue;
            }

            double score =
                    (double) timeline.duration()
                            .toMillis()
                            / config.slowTraceThreshold()
                            .toMillis();

            anomalies.add(
                    new Anomaly(
                            AnomalyType.SLOW_TRACE,
                            calculateSeverity(score),
                            timeline.rootCauseFingerprint(),
                            timeline.traceId(),
                            "Trace took "
                                    + timeline.duration(),
                            score,
                            timeline.endTime()
                    )
            );
        }

        return anomalies;
    }

    private List<Anomaly> detectNewFingerprints(
            List<LogEventAnalysis> analyses,
            ErrorGroupingResult groupingResult) {

        /*
         * Phase 8 currently has no historical database.
         *
         * Therefore we cannot know whether a fingerprint
         * existed yesterday or last week.
         *
         * We detect fingerprints that appear only once
         * in the current log window.
         */

        List<Anomaly> anomalies =
                new ArrayList<>();

        for (var group :
                groupingResult.groups()) {

            if (group.occurrenceCount() != 1) {
                continue;
            }

            anomalies.add(
                    new Anomaly(
                            AnomalyType.NEW_FINGERPRINT,
                            AnomalySeverity.LOW,
                            group.fingerprint(),
                            null,
                            "Fingerprint appeared only once "
                                    + "in the analyzed log window",
                            1.0,
                            group.lastOccurrence()
                    )
            );
        }

        return anomalies;
    }

    private boolean isError(
            LogEventAnalysis analysis) {

        String level =
                String.valueOf(analysis.event().level());

        return "ERROR".equalsIgnoreCase(level)
                || "FATAL".equalsIgnoreCase(level);
    }

    private Instant timestamp(
            LogEventAnalysis analysis) {

        return analysis.event()
                .timestamp().toInstant();
    }

    private AnomalySeverity calculateSeverity(
            double score) {

        if (score >= 10) {
            return AnomalySeverity.CRITICAL;
        }

        if (score >= 5) {
            return AnomalySeverity.HIGH;
        }

        if (score >= 2) {
            return AnomalySeverity.MEDIUM;
        }

        return AnomalySeverity.LOW;
    }
}