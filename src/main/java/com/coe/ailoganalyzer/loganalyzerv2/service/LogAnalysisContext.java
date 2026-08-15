package com.coe.ailoganalyzer.loganalyzerv2.service;

import com.coe.ailoganalyzer.loganalyzerv2.model.Anomaly;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroupingResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import com.coe.ailoganalyzer.loganalyzerv2.model.TraceTimeline;

import java.util.List;

public record LogAnalysisContext(

        List<LogEventAnalysis> analyses,

        ErrorGroupingResult grouping,

        List<TraceTimeline> timelines,

        List<Anomaly> anomalies

) {
}