package com.coe.ailoganalyzer.loganalyzerv2.report;

import com.coe.ailoganalyzer.loganalyzerv2.model.*;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentReportService {
    public IncidentReport generate(
            LogAnalysisContext context,
            RcaAnalysisResult rca) {

        return generate(
                context.analyses(),
                context.grouping(),
                context.timelines(),
                context.anomalies(),
                rca
        );
    }
    public IncidentReport generate(
            List<LogEventAnalysis> analyses,
            ErrorGroupingResult grouping,
            List<TraceTimeline> timelines,
            List<Anomaly> anomalies,
            RcaAnalysisResult rca) {

        Instant startTime =
                analyses.stream()
                        .map(analysis ->
                                analysis.event().timestamp())
                        .min(Comparator.naturalOrder())
                        .orElse(null).toInstant();

        Instant endTime =
                analyses.stream()
                        .map(analysis ->
                                analysis.event().timestamp())
                        .max(Comparator.naturalOrder())
                        .orElse(null).toInstant();

        Duration duration =
                calculateDuration(
                        startTime,
                        endTime
                );

        int totalErrors =
                (int) analyses.stream()
                        .filter(this::isError)
                        .count();

        int affectedTraces =
                (int) timelines.stream()
                        .filter(this::hasErrors)
                        .count();

        List<String> fingerprints =
                grouping.groups()
                        .stream()
                        .map(ErrorGroup::fingerprint)
                        .toList();

        AnomalySeverity severity =
                determineSeverity(
                        grouping,
                        anomalies
                );

        String title =
                generateTitle(
                        severity,
                        rca
                );

        String summary =
                generateSummary(
                        totalErrors,
                        affectedTraces,
                        anomalies,
                        rca
                );

        List<String> recommendations =
                extractRecommendations(
                        rca
                );

        return new IncidentReport(
                generateIncidentId(),
                title,
                severity,
                summary,
                startTime,
                endTime,
                duration,
                analyses.size(),
                totalErrors,
                timelines.size(),
                affectedTraces,
                fingerprints,
                anomalies,
                rca,
                recommendations
        );
    }

    private String generateIncidentId() {

        return "INC-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    private Duration calculateDuration(
            Instant start,
            Instant end) {

        if (start == null || end == null) {
            return Duration.ZERO;
        }

        return Duration.between(
                start,
                end
        );
    }

    private boolean isError(
            LogEventAnalysis analysis) {

        String level =
                String.valueOf(analysis.event().level());

        return "ERROR".equalsIgnoreCase(level)
                || "FATAL".equalsIgnoreCase(level);
    }

    private boolean hasErrors(
            TraceTimeline timeline) {

        return timeline.errorCount() > 0;
    }

    private AnomalySeverity determineSeverity(
            ErrorGroupingResult grouping,
            List<Anomaly> anomalies) {

        AnomalySeverity highest =
                AnomalySeverity.LOW;

        for (Anomaly anomaly : anomalies) {

            if (severityRank(anomaly.severity())
                    > severityRank(highest)) {

                highest = anomaly.severity();
            }
        }

        return highest;
    }
    private int severityRank(
            AnomalySeverity severity) {

        return switch (severity) {

            case LOW -> 1;

            case MEDIUM -> 2;

            case HIGH -> 3;

            case CRITICAL -> 4;
        };
    }

    private String generateTitle(
            AnomalySeverity severity,
            RcaAnalysisResult rca) {

        if (rca.successful()
                && rca.result() != null
                && rca.result().rootCause() != null) {

            return severity
                    + " Incident - "
                    + rca.result().rootCause();
        }

        return severity
                + " Application Incident";
    }

    private String generateSummary(
            int totalErrors,
            int affectedTraces,
            List<Anomaly> anomalies,
            RcaAnalysisResult rca) {

        if (rca.successful()
                && rca.result() != null) {

            return rca.result()
                    .rootCause()
                    + ". "
                    + rca.result().impact();
        }

        return "Detected "
                + totalErrors
                + " errors affecting "
                + affectedTraces
                + " traces with "
                + anomalies.size()
                + " anomalies.";
    }

    private List<String> extractRecommendations(
            RcaAnalysisResult rca) {

        if (!rca.successful()
                || rca.result() == null
                || rca.result().recommendations() == null) {

            return List.of();
        }

        return rca.result()
                .recommendations();
    }
}