package com.coe.ailoganalyzer.loganalyzerv2.parser.strategies;

import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import com.coe.ailoganalyzer.loganalyzerv2.parser.LogParser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GenericLogParser implements LogParser {

    @Override
    public boolean supports(String logEntry) {
        return true;
    }

    @Override
    public LogEvent parse(String logEntry) {

        return new LogEvent(
                null,
                detectLevel(logEntry),
                LogFormat.GENERIC,
                null,
                null,
                null,
                logEntry,
                null,
                null,
                null,
                logEntry,
                Map.of()
        );
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