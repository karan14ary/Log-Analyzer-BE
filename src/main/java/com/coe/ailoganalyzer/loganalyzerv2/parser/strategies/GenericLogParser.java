package com.coe.ailoganalyzer.loganalyzerv2.parser.strategies;

import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import com.coe.ailoganalyzer.loganalyzerv2.parser.LogParser;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GenericLogParser implements LogParser {

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?)"
    );

    @Override
    public boolean supports(String logEntry) {
        return true;
    }

    @Override
    public LogEvent parse(String logEntry) {
        return new LogEvent(
                parseTimestamp(logEntry),
                detectLevel(logEntry),
                LogFormat.GENERIC,
                null,
                null,
                null,
                logEntry,
                extractTraceId(logEntry),
                extractSpanId(logEntry),
                extractCorrelationId(logEntry),
                logEntry,
                Map.of()
        );
    }

    private OffsetDateTime parseTimestamp(String logEntry) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(logEntry);
        if (matcher.find()) {
            String tsStr = matcher.group(1);
            try {
                return OffsetDateTime.parse(tsStr.replace(" ", "T"));
            } catch (Exception ignored) {
                try {
                    return OffsetDateTime.parse(tsStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                } catch (Exception ignored2) {}
            }
        }
        return null;
    }

    private String extractTraceId(String log) {
        return extract(log, "(?i)traceId[=:]\\s*([\\w-]+)");
    }

    private String extractSpanId(String log) {
        return extract(log, "(?i)spanId[=:]\\s*([\\w-]+)");
    }

    private String extractCorrelationId(String log) {
        return extract(log, "(?i)(?:correlationId|correlation-id)[=:]\\s*([\\w-]+)");
    }

    private String extract(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private LogLevel detectLevel(String log) {

        String value =
                log.toUpperCase();

        if (value.contains("FATAL")) {
            return LogLevel.FATAL;
        }

        if (value.contains("ERROR")) {
            return LogLevel.ERROR;
        }

        if (value.contains("WARN")) {
            return LogLevel.WARN;
        }

        if (value.contains("DEBUG")) {
            return LogLevel.DEBUG;
        }

        if (value.contains("TRACE")) {
            return LogLevel.TRACE;
        }

        if (value.contains("INFO")) {
            return LogLevel.INFO;
        }

        return LogLevel.UNKNOWN;
    }
}