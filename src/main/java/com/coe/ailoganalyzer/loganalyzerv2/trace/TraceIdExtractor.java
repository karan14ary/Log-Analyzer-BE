package com.coe.ailoganalyzer.loganalyzerv2.trace;


import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TraceIdExtractor {

    private static final Pattern TRACE_ID_PATTERN =
            Pattern.compile(
                    "(?i)\\btrace[_-]?id\\s*[=:]\\s*([\\w-]+)"
            );

    private static final Pattern CORRELATION_ID_PATTERN =
            Pattern.compile(
                    "(?i)\\bcorrelation[_-]?id\\s*[=:]\\s*([\\w-]+)"
            );

    public String extractTraceId(String log) {

        if (log == null || log.isBlank()) {
            return null;
        }

        Matcher matcher =
                TRACE_ID_PATTERN.matcher(log);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    public String extractCorrelationId(String log) {

        if (log == null || log.isBlank()) {
            return null;
        }

        Matcher matcher =
                CORRELATION_ID_PATTERN.matcher(log);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}