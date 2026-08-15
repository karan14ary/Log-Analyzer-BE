package com.coe.ailoganalyzer.loganalyzerv2.parser;


import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;

public interface LogParser {

    boolean supports(String logEntry);

    LogEvent parse(String logEntry);
}