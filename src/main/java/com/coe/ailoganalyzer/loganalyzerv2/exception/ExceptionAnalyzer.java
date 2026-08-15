package com.coe.ailoganalyzer.loganalyzerv2.exception;
import com.coe.ailoganalyzer.loganalyzerv2.model.ExceptionInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.StackFrame;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExceptionAnalyzer {

    private static final Pattern EXCEPTION_PATTERN =
            Pattern.compile(
                    "^(?:Caused by:\\s+)?"
                            + "([a-zA-Z_$][\\w$]*(?:\\.[\\w$]+)*)"
                            + "(?::\\s*(.*))?$"
            );

    private static final Pattern STACK_FRAME_PATTERN =
            Pattern.compile(
                    "^at\\s+"
                            + "([\\w$\\.]+)"
                            + "\\.([\\w$<>]+)"
                            + "\\((.*?):(\\d+)\\)$"
            );

    private static final Pattern NATIVE_STACK_FRAME_PATTERN =
            Pattern.compile(
                    "^at\\s+"
                            + "([\\w$\\.]+)"
                            + "\\.([\\w$<>]+)"
                            + "\\(Native Method\\)$"
            );
    private static final Pattern SUPPRESSED_PATTERN =
            Pattern.compile(
                    "^Suppressed:\\s+"
                            + "([\\w$\\.]+)"
                            + "(?::\\s*(.*))?$"
            );

    public ExceptionInfo analyze(String stackTrace) {

        if (stackTrace == null || stackTrace.isBlank()) {
            return new ExceptionInfo(
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null,
                    null
            );
        }

        List<String> lines =
                stackTrace.lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .toList();

        List<String> causeChain =
                new ArrayList<>();

        List<StackFrame> stackFrames =
                new ArrayList<>();

        String exceptionType = null;
        String message = null;

        String rootCauseType = null;
        String rootCauseMessage = null;

        for (String line : lines) {

            Matcher exceptionMatcher =
                    EXCEPTION_PATTERN.matcher(line);

            if (exceptionMatcher.matches()
                    && looksLikeException(
                    exceptionMatcher.group(1))) {

                String type =
                        exceptionMatcher.group(1);

                String exceptionMessage =
                        exceptionMatcher.group(2);

                if (exceptionType == null) {
                    exceptionType = type;
                    message = exceptionMessage;
                }

                causeChain.add(type);

                rootCauseType = type;
                rootCauseMessage = exceptionMessage;

                continue;
            }

            Matcher frameMatcher =
                    STACK_FRAME_PATTERN.matcher(line);

            if (frameMatcher.matches()) {

                stackFrames.add(
                        new StackFrame(
                                frameMatcher.group(1),
                                frameMatcher.group(2),
                                frameMatcher.group(3),
                                Integer.valueOf(
                                        frameMatcher.group(4)
                                ),
                                false
                        )
                );

                continue;
            }
            Matcher suppressedMatcher =
                    SUPPRESSED_PATTERN.matcher(line);

            if (suppressedMatcher.matches()) {

                causeChain.add(
                        suppressedMatcher.group(1)
                );

                continue;
            }
            Matcher nativeMatcher =
                    NATIVE_STACK_FRAME_PATTERN
                            .matcher(line);

            if (nativeMatcher.matches()) {

                stackFrames.add(
                        new StackFrame(
                                nativeMatcher.group(1),
                                nativeMatcher.group(2),
                                null,
                                null,
                                true
                        )
                );
            }
        }

        return new ExceptionInfo(
                exceptionType,
                message,
                stackFrames,
                causeChain,
                rootCauseType,
                rootCauseMessage
        );
    }

    private boolean looksLikeException(
            String value) {

        return value.endsWith("Exception")
                || value.endsWith("Error")
                || value.endsWith("Throwable")
                || value.contains("Exception");
    }
}