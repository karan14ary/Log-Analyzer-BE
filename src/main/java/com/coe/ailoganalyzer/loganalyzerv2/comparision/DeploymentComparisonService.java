package com.coe.ailoganalyzer.loganalyzerv2.comparision;


import com.coe.ailoganalyzer.loganalyzerv2.model.*;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DeploymentComparisonService {
    public DeploymentComparison compare(
            LogAnalysisContext before,
            LogAnalysisContext after) {

        MetricComparison metrics =
                compareMetrics(
                        before,
                        after
                );

        ErrorComparison errors =
                compareErrors(
                        before,
                        after
                );

        AnomalyComparison anomalies =
                compareAnomalies(
                        before,
                        after
                );

        int improvements =
                calculateImprovements(
                        metrics,
                        errors,
                        anomalies
                );

        int regressions =
                calculateRegressions(
                        metrics,
                        errors,
                        anomalies
                );

        ComparisonStatus status =
                determineStatus(
                        improvements,
                        regressions
                );

        DeploymentComparisonSummary summary =
                new DeploymentComparisonSummary(
                        status,
                        generateMessage(status),
                        improvements,
                        regressions
                );

        List<String> recommendations =
                generateRecommendations(
                        status,
                        errors,
                        metrics
                );

        return new DeploymentComparison(
                summary,
                metrics,
                errors,
                anomalies,
                recommendations
        );
    }

    private MetricComparison compareMetrics(
            LogAnalysisContext before,
            LogAnalysisContext after) {

        int beforeEvents =
                before.analyses().size();

        int afterEvents =
                after.analyses().size();

        int beforeErrors =
                countErrors(
                        before.analyses()
                );

        int afterErrors =
                countErrors(
                        after.analyses()
                );

        double beforeErrorRate =
                calculateErrorRate(
                        beforeEvents,
                        beforeErrors
                );

        double afterErrorRate =
                calculateErrorRate(
                        afterEvents,
                        afterErrors
                );

        return new MetricComparison(

                beforeEvents,

                afterEvents,

                afterEvents - beforeEvents,

                beforeErrors,

                afterErrors,

                afterErrors - beforeErrors,

                beforeErrorRate,

                afterErrorRate,

                afterErrorRate - beforeErrorRate,

                before.timelines().size(),

                after.timelines().size(),

                countAffectedTraces(
                        before.timelines()
                ),

                countAffectedTraces(
                        after.timelines()
                )
        );
    }

    private int countErrors(
            List<LogEventAnalysis> analyses) {

        return (int) analyses.stream()
                .filter(this::isError)
                .count();
    }

    private boolean isError(
            LogEventAnalysis analysis) {

        String level =
                String.valueOf(analysis.event().level());

        return "ERROR".equalsIgnoreCase(level)
                || "FATAL".equalsIgnoreCase(level);
    }

    private double calculateErrorRate(
            int events,
            int errors) {

        if (events == 0) {
            return 0.0;
        }

        return (double) errors / events;
    }

    private int countAffectedTraces(
            List<TraceTimeline> timelines) {

        return (int) timelines.stream()
                .filter(timeline ->
                        timeline.errorCount() > 0)
                .count();
    }
    private ErrorComparison compareErrors(
            LogAnalysisContext before,
            LogAnalysisContext after) {

        Map<String, Integer> beforeCounts =
                fingerprintCounts(
                        before
                );

        Map<String, Integer> afterCounts =
                fingerprintCounts(
                        after
                );

        Set<String> beforeFingerprints =
                beforeCounts.keySet();

        Set<String> afterFingerprints =
                afterCounts.keySet();

        List<String> newErrors =
                afterFingerprints.stream()
                        .filter(fp ->
                                !beforeFingerprints
                                        .contains(fp))
                        .toList();

        List<String> resolvedErrors =
                beforeFingerprints.stream()
                        .filter(fp ->
                                !afterFingerprints
                                        .contains(fp))
                        .toList();

        List<String> persistentErrors =
                afterFingerprints.stream()
                        .filter(beforeFingerprints::contains)
                        .toList();

        List<String> increasedErrors =
                persistentErrors.stream()
                        .filter(fp ->
                                afterCounts.get(fp)
                                        > beforeCounts.get(fp))
                        .toList();

        List<String> decreasedErrors =
                persistentErrors.stream()
                        .filter(fp ->
                                afterCounts.get(fp)
                                        < beforeCounts.get(fp))
                        .toList();

        return new ErrorComparison(
                newErrors,
                resolvedErrors,
                persistentErrors,
                increasedErrors,
                decreasedErrors
        );
    }
    private Map<String, Integer> fingerprintCounts(
            LogAnalysisContext context) {

        Map<String, Integer> counts =
                new HashMap<>();

        context.grouping()
                .groups()
                .forEach(group ->
                        counts.put(
                                group.fingerprint(),
                                group.occurrenceCount()
                        )
                );

        return counts;
    }
    private AnomalyComparison compareAnomalies(
            LogAnalysisContext before,
            LogAnalysisContext after) {

        Set<String> beforeTypes =
                anomalyTypes(
                        before.anomalies()
                );

        Set<String> afterTypes =
                anomalyTypes(
                        after.anomalies()
                );

        List<String> newAnomalies =
                afterTypes.stream()
                        .filter(type ->
                                !beforeTypes.contains(type))
                        .toList();

        List<String> resolvedAnomalies =
                beforeTypes.stream()
                        .filter(type ->
                                !afterTypes.contains(type))
                        .toList();

        List<String> persistentAnomalies =
                afterTypes.stream()
                        .filter(beforeTypes::contains)
                        .toList();

        return new AnomalyComparison(
                newAnomalies,
                resolvedAnomalies,
                persistentAnomalies
        );
    }

    private Set<String> anomalyTypes(
            List<Anomaly> anomalies) {

        Set<String> types =
                new HashSet<>();

        for (Anomaly anomaly : anomalies) {

            types.add(
                    anomaly.type().toString()
            );
        }

        return types;
    }
    private int calculateImprovements(
            MetricComparison metrics,
            ErrorComparison errors,
            AnomalyComparison anomalies) {

        int score = 0;

        if (metrics.afterErrors()
                < metrics.beforeErrors()) {

            score++;
        }

        if (metrics.afterErrorRate()
                < metrics.beforeErrorRate()) {

            score++;
        }

        score += errors.resolvedErrors().size();

        score += errors.decreasedErrors().size();

        score += anomalies.resolvedAnomalies().size();

        return score;
    }
    private int calculateRegressions(
            MetricComparison metrics,
            ErrorComparison errors,
            AnomalyComparison anomalies) {

        int score = 0;

        if (metrics.afterErrors()
                > metrics.beforeErrors()) {

            score++;
        }

        if (metrics.afterErrorRate()
                > metrics.beforeErrorRate()) {

            score++;
        }

        score += errors.newErrors().size();

        score += errors.increasedErrors().size();

        score += anomalies.newAnomalies().size();

        return score;
    }
    private ComparisonStatus determineStatus(
            int improvements,
            int regressions) {

        if (improvements == 0
                && regressions == 0) {

            return ComparisonStatus.UNCHANGED;
        }

        if (improvements > 0
                && regressions == 0) {

            return ComparisonStatus.IMPROVED;
        }

        if (regressions > 0
                && improvements == 0) {

            return ComparisonStatus.REGRESSED;
        }

        return ComparisonStatus.MIXED;
    }
    private String generateMessage(
            ComparisonStatus status) {

        return switch (status) {

            case IMPROVED ->
                    "The deployment shows an overall improvement.";

            case REGRESSED ->
                    "The deployment shows an overall regression.";

            case MIXED ->
                    "The deployment shows both improvements and regressions.";

            case UNCHANGED ->
                    "No significant change was detected.";
        };
    }
    private List<String> generateRecommendations(
            ComparisonStatus status,
            ErrorComparison errors,
            MetricComparison metrics) {

        List<String> recommendations =
                new java.util.ArrayList<>();

        if (status == ComparisonStatus.REGRESSED
                || status == ComparisonStatus.MIXED) {

            if (!errors.newErrors().isEmpty()) {

                recommendations.add(
                        "Investigate newly introduced error fingerprints."
                );
            }

            if (!errors.increasedErrors().isEmpty()) {

                recommendations.add(
                        "Investigate error fingerprints whose frequency increased after deployment."
                );
            }

            if (metrics.afterErrorRate()
                    > metrics.beforeErrorRate()) {

                recommendations.add(
                        "Review deployment changes because the error rate increased."
                );
            }
        }

        if (status == ComparisonStatus.IMPROVED) {

            recommendations.add(
                    "Deployment appears to have improved application stability."
            );
        }

        return recommendations;
    }
}
