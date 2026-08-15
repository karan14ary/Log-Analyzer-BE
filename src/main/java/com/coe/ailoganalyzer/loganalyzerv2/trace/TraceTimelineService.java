package com.coe.ailoganalyzer.loganalyzerv2.trace;

import com.coe.ailoganalyzer.loganalyzerv2.model.FingerprintInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import com.coe.ailoganalyzer.loganalyzerv2.model.TraceTimeline;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class TraceTimelineService {

    public List<TraceTimeline> buildTimelines(
            List<LogEventAnalysis> analyses) {

        Map<String, List<LogEventAnalysis>> traces =
                new LinkedHashMap<>();

        for (LogEventAnalysis analysis : analyses) {

            String traceId =
                    analysis.traceId();

            if (traceId == null
                    || traceId.isBlank()) {

                continue;
            }

            traces
                    .computeIfAbsent(
                            traceId,
                            ignored -> new ArrayList<>()
                    )
                    .add(analysis);
        }

        return traces.values()
                .stream()
                .map(this::buildTimeline)
                .sorted(
                        Comparator.comparing(
                                TraceTimeline::startTime
                        )
                )
                .toList();
    }

    private TraceTimeline buildTimeline(
            List<LogEventAnalysis> events) {

        List<LogEventAnalysis> sortedEvents =
                events.stream()
                        .sorted(
                                Comparator.comparing(
                                        analysis ->
                                                analysis.event()
                                                        .timestamp()
                                )
                        )
                        .toList();

        Instant startTime =
                sortedEvents.getFirst()
                        .event()
                        .timestamp().toInstant();

        Instant endTime =
                sortedEvents.getLast()
                        .event()
                        .timestamp().toInstant();

        Duration duration =
                Duration.between(
                        startTime,
                        endTime
                );

        int errorCount =
                (int) sortedEvents.stream()
                        .filter(this::isError)
                        .count();

        String traceId =
                sortedEvents.getFirst()
                        .traceId();

        String correlationId =
                sortedEvents.stream()
                        .map(LogEventAnalysis::correlationId)
                        .filter(id ->
                                id != null
                                        && !id.isBlank()
                        )
                        .findFirst()
                        .orElse(null);

        String rootCauseFingerprint =
                sortedEvents.stream()
                        .filter(this::isError)
                        .map(LogEventAnalysis::fingerprintInfo)
                        .map(FingerprintInfo::fingerprint)
                        .findFirst()
                        .orElse(null);

        return new TraceTimeline(
                traceId,
                correlationId,
                startTime,
                endTime,
                duration,
                sortedEvents.size(),
                errorCount,
                rootCauseFingerprint,
                sortedEvents

        );
    }
    private String getRootCauseCandidate(
            LogEventAnalysis analysis) {

        if (analysis.exceptionInfo() != null
                && analysis.exceptionInfo()
                .exceptionType() != null) {

            return analysis.fingerprintInfo()
                    .fingerprint();
        }

        return analysis.fingerprintInfo()
                .fingerprint();
    }
    private String findRootCauseFingerprint(
            List<LogEventAnalysis> events) {

        return events.stream()
                .filter(this::isError)
                .sorted(
                        Comparator.comparing(
                                analysis ->
                                        analysis.event()
                                                .timestamp()
                        )
                )
                .map(this::getRootCauseCandidate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
    private boolean isError(
            LogEventAnalysis analysis) {

        String level =
                String.valueOf(analysis.event().level());

        return "ERROR".equalsIgnoreCase(level)
                || "FATAL".equalsIgnoreCase(level);
    }
   }