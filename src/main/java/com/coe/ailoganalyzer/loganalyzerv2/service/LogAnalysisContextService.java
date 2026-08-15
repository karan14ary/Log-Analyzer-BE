package com.coe.ailoganalyzer.loganalyzerv2.service;


import com.coe.ailoganalyzer.loganalyzerv2.anomaly.AnomalyDetectionService;
import com.coe.ailoganalyzer.loganalyzerv2.grouping.ErrorGroupingService;
import com.coe.ailoganalyzer.loganalyzerv2.model.AnomalyDetectionResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import com.coe.ailoganalyzer.loganalyzerv2.model.TraceTimeline;
import com.coe.ailoganalyzer.loganalyzerv2.parser.LogEntrySplitter;
import com.coe.ailoganalyzer.loganalyzerv2.parser.strategies.LogParsingEngine;
import com.coe.ailoganalyzer.loganalyzerv2.severity.SeverityAnalysisService;
import com.coe.ailoganalyzer.loganalyzerv2.trace.TraceTimelineService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class LogAnalysisContextService {

    private final LogEntrySplitter entrySplitter;

    private final LogParsingEngine parsingEngine;

    private final LogAnalysisPipeline pipeline;

    private final ErrorGroupingService errorGroupingService;

    private final SeverityAnalysisService
            severityAnalysisService;

    private final TraceTimelineService
            traceTimelineService;

    private final AnomalyDetectionService
            anomalyDetectionService;

    public LogAnalysisContextService(
            LogEntrySplitter entrySplitter,
            LogParsingEngine parsingEngine,
            LogAnalysisPipeline pipeline,
            ErrorGroupingService errorGroupingService,
            SeverityAnalysisService severityAnalysisService,
            TraceTimelineService traceTimelineService,
            AnomalyDetectionService anomalyDetectionService) {

        this.entrySplitter = entrySplitter;
        this.parsingEngine = parsingEngine;
        this.pipeline = pipeline;
        this.errorGroupingService =
                errorGroupingService;
        this.severityAnalysisService =
                severityAnalysisService;
        this.traceTimelineService =
                traceTimelineService;
        this.anomalyDetectionService =
                anomalyDetectionService;
    }

    public LogAnalysisContext analyze(
            MultipartFile file)
            throws IOException {

        String content =
                new String(
                        file.getBytes(),
                        StandardCharsets.UTF_8
                );

        var events =
                entrySplitter
                        .split(content)
                        .stream()
                        .map(parsingEngine::parse)
                        .toList();

        List<LogEventAnalysis> analyses =
                events.stream()
                        .map(pipeline::analyze)
                        .toList();

        var grouping =
                errorGroupingService.group(
                        analyses
                );

        grouping =
                severityAnalysisService.analyze(
                        grouping
                );

        List<TraceTimeline> timelines =
                traceTimelineService.buildTimelines(
                        analyses
                );

        AnomalyDetectionResult anomalyResult =
                anomalyDetectionService.detect(
                        analyses,
                        grouping,
                        timelines
                );

        return new LogAnalysisContext(
                analyses,
                grouping,
                timelines,
                anomalyResult.anomalies()
        );
    }
}