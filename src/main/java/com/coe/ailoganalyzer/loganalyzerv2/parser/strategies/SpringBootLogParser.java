package com.coe.ailoganalyzer.loganalyzerv2.parser.strategies;

import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import com.coe.ailoganalyzer.loganalyzerv2.parser.LogParser;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SpringBootLogParser implements LogParser {

    private static final Pattern PATTERN =
            Pattern.compile(
                    "^(\\S+)\\s+" +
                            "(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\s+" +
                            "(\\d+)\\s+---\\s+" +
                            "\\[(.*?)]\\s+" +
                            "(\\S+)\\s+:\\s+" +
                            "(.*)$"
            );

    @Override
    public boolean supports(String logEntry) {

        String firstLine =
                logEntry.split("\\R", 2)[0];

        return PATTERN.matcher(firstLine).matches();
    }

    @Override
    public LogEvent parse(String logEntry) {

        String firstLine =
                logEntry.split("\\R", 2)[0];

        Matcher matcher =
                PATTERN.matcher(firstLine);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid Spring Boot log format"
            );
        }

        return new LogEvent(
                parseTimestamp(matcher.group(1)),
                LogLevel.valueOf(matcher.group(2)),
                LogFormat.SPRING_BOOT,
                null,
                matcher.group(4),
                matcher.group(5),
                matcher.group(6),
                extractTraceId(logEntry),
                extractSpanId(logEntry),
                extractCorrelationId(logEntry),
                logEntry,
                Map.of()
        );
    }

    private OffsetDateTime parseTimestamp(
            String timestamp) {

        try {
            return OffsetDateTime.parse(timestamp);
        } catch (Exception ignored) {
        }

        LocalDateTime localDateTime =
                LocalDateTime.parse(
                        timestamp,
                        java.time.format.DateTimeFormatter
                                .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                );

        return localDateTime.atOffset(
                ZoneOffset.UTC
        );
    }

    private String extractTraceId(String log) {
        return extract(
                log,
                "(?i)traceId[=:]\\s*([\\w-]+)"
        );
    }

    private String extractSpanId(String log) {
        return extract(
                log,
                "(?i)spanId[=:]\\s*([\\w-]+)"
        );
    }

    private String extractCorrelationId(String log) {
        return extract(
                log,
                "(?i)(?:correlationId|correlation-id)[=:]\\s*([\\w-]+)"
        );
    }

    private String extract(
            String text,
            String regex) {

        Matcher matcher =
                Pattern.compile(regex)
                        .matcher(text);

        return matcher.find()
                ? matcher.group(1)
                : null;
    }
}