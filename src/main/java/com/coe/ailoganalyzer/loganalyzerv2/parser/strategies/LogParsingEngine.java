package com.coe.ailoganalyzer.loganalyzerv2.parser.strategies;


import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.parser.LogParser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LogParsingEngine {

    private final List<LogParser> parsers;

    public LogParsingEngine(List<LogParser> parsers) {
        this.parsers = parsers;
    }

    public LogEvent parse(String logEntry) {

        return parsers.stream()
                .filter(parser -> parser.supports(logEntry))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No parser available"
                        )
                )
                .parse(logEntry);
    }
}