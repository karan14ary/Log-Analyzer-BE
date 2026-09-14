package com.coe.ailoganalyzer.loganalyzerv2.grouping;

import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroup;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroupingResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ErrorGroupingService {

    private static final int MAX_SAMPLE_EVENTS = 20;

    public ErrorGroupingResult group(
            List<LogEventAnalysis> analyses) {

        if (analyses == null || analyses.isEmpty()) {

            return new ErrorGroupingResult(
                    0,
                    0,
                    List.of()
            );
        }

        Map<String, List<LogEventAnalysis>> grouped =
                new LinkedHashMap<>();

        for (LogEventAnalysis analysis : analyses) {

            String fingerprint =
                    analysis.fingerprintInfo()
                            .fingerprint();

            grouped
                    .computeIfAbsent(
                            fingerprint,
                            key -> new ArrayList<>()
                    )
                    .add(analysis);
        }

        List<ErrorGroup> groups =
                grouped.values()
                        .stream()
                        .map(this::createGroup)
                        .sorted(
                                Comparator.comparingInt(
                                        ErrorGroup::occurrenceCount
                                ).reversed()
                        )
                        .toList();

        return new ErrorGroupingResult(
                analyses.size(),
                groups.size(),
                groups
        );
    }

    private ErrorGroup createGroup(
            List<LogEventAnalysis> events) {

        LogEventAnalysis first =
                events.getFirst();

        String fingerprint =
                first.fingerprintInfo()
                        .fingerprint();

        String normalizedMessage =
                first.fingerprintInfo()
                        .normalizedMessage();

        Instant firstOccurrence =
                events.stream()
                        .map(this::timestamp)
                        .min(Comparator.naturalOrder())
                        .orElse(null);

        Instant lastOccurrence =
                events.stream()
                        .map(this::timestamp)
                        .max(Comparator.naturalOrder())
                        .orElse(null);

        /*
         * Keep only representative events.
         * We still preserve the total occurrence count
         * using events.size().
         */
        List<LogEventAnalysis> samples =
                events.stream()
                        .limit(MAX_SAMPLE_EVENTS)
                        .toList();

        return new ErrorGroup(
                fingerprint,
                normalizedMessage,
                events.size(),
                firstOccurrence,
                lastOccurrence,
                samples
        );
    }

    private Instant timestamp(
            LogEventAnalysis analysis) {

        if (analysis.event().timestamp() == null) {
            return Instant.now();
        }

        return analysis.event()
                .timestamp().toInstant();
    }
}