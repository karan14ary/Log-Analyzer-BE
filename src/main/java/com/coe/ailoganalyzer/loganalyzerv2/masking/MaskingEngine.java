package com.coe.ailoganalyzer.loganalyzerv2.masking;

import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingFinding;
import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;

@Component
public class MaskingEngine {

    private final List<MaskingRule> rules;

    public MaskingEngine(List<MaskingRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public MaskingResult mask(String text) {

        if (text == null || text.isBlank()) {
            return new MaskingResult(
                    text,
                    List.of()
            );
        }

        List<MaskMatch> matches =
                findMatches(text);

        List<MaskMatch> resolvedMatches =
                resolveOverlaps(matches);

        String maskedText =
                applyReplacements(
                        text,
                        resolvedMatches
                );

        List<MaskingFinding> findings =
                buildFindings(resolvedMatches);

        return new MaskingResult(
                maskedText,
                findings
        );
    }

    private List<MaskMatch> findMatches(
            String text) {

        List<MaskMatch> matches =
                new ArrayList<>();

        for (MaskingRule rule : rules) {

            Matcher matcher =
                    rule.pattern()
                            .matcher(text);

            int occurrence = 0;

            while (matcher.find()) {

                occurrence++;

                matches.add(
                        new MaskMatch(
                                matcher.start(),
                                matcher.end(),
                                matcher.group(),
                                rule.type(),
                                rule.replacement(
                                        matcher.group()
                                ),
                                occurrence
                        )
                );
            }
        }

        return matches;
    }

    private List<MaskMatch> resolveOverlaps(
            List<MaskMatch> matches) {

        /*
         * Sort by:
         *
         * 1. Start position
         * 2. Longest match first
         *
         * This gives deterministic behaviour when
         * multiple rules match the same location.
         */
        List<MaskMatch> sorted =
                matches.stream()
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                MaskMatch::start
                                        )
                                        .thenComparing(
                                                Comparator
                                                        .comparingInt(
                                                                MaskMatch::length
                                                        )
                                                        .reversed()
                                        )
                        )
                        .toList();

        List<MaskMatch> resolved =
                new ArrayList<>();

        for (MaskMatch candidate : sorted) {

            boolean overlaps =
                    resolved.stream()
                            .anyMatch(
                                    existing ->
                                            overlaps(
                                                    existing,
                                                    candidate
                                            )
                            );

            if (!overlaps) {
                resolved.add(candidate);
            }
        }

        return resolved.stream()
                .sorted(
                        Comparator.comparingInt(
                                MaskMatch::start
                        )
                )
                .toList();
    }

    private boolean overlaps(
            MaskMatch first,
            MaskMatch second) {

        return first.start() < second.end()
                && second.start() < first.end();
    }

    private String applyReplacements(
            String original,
            List<MaskMatch> matches) {

        StringBuilder result =
                new StringBuilder();

        int currentPosition = 0;

        for (MaskMatch match : matches) {

            result.append(
                    original,
                    currentPosition,
                    match.start()
            );

            result.append(
                    match.replacement()
            );

            currentPosition =
                    match.end();
        }

        result.append(
                original,
                currentPosition,
                original.length()
        );

        return result.toString();
    }

    private List<MaskingFinding> buildFindings(
            List<MaskMatch> matches) {

        return matches.stream()
                .map(match ->
                        new MaskingFinding(
                                match.type(),
                                match.replacement(),
                                match.occurrence()
                        )
                )
                .toList();
    }

    private record MaskMatch(

            int start,

            int end,

            String originalValue,

           MaskingType type,

            String replacement,

            int occurrence

    ) {

        int length() {
            return end - start;
        }
    }
}