package com.coe.ailoganalyzer.loganalyzerv2.service;

import com.coe.ailoganalyzer.loganalyzerv2.ai.RcaService;
import com.coe.ailoganalyzer.loganalyzerv2.anomaly.AnomalyDetectionService;
import com.coe.ailoganalyzer.loganalyzerv2.exception.ExceptionAnalysisService;
import com.coe.ailoganalyzer.loganalyzerv2.grouping.ErrorGroupingService;
import com.coe.ailoganalyzer.loganalyzerv2.model.*;
import com.coe.ailoganalyzer.loganalyzerv2.parser.LogEntrySplitter;
import com.coe.ailoganalyzer.loganalyzerv2.parser.strategies.LogParsingEngine;
import com.coe.ailoganalyzer.loganalyzerv2.severity.SeverityAnalysisService;
import com.coe.ailoganalyzer.loganalyzerv2.trace.TraceTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogAnalysisService {

    private final LogEntrySplitter entrySplitter;
    private final LogParsingEngine parsingEngine;
    private final ExceptionAnalysisService exceptionAnalysisService;
    private final LogAnalysisPipeline logAnalysisPipeline;
    private final ErrorGroupingService errorGroupingService;
    private final SeverityAnalysisService severityAnalysisService;
    private final TraceTimelineService traceTimelineService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final RcaService rcaService;

    public List<LogEvent> parse(
            MultipartFile file) throws IOException {

        String content =
                new String(
                        file.getBytes(),
                        StandardCharsets.UTF_8
                );

        return entrySplitter
                .split(content)
                .stream()
                .map(parsingEngine::parse)
                .toList();
    }
    public List<LogEventAnalysis> analyzeExceptions(
            MultipartFile file) throws IOException {

        String content =
                new String(
                        file.getBytes(),
                        StandardCharsets.UTF_8
                );

        return entrySplitter
                .split(content)
                .stream()
                .map(parsingEngine::parse)
                .map(exceptionAnalysisService::analyze)
                .toList();
    }
    public List<LogEventAnalysis> analyze(
            MultipartFile file) throws IOException {

        String content =
                new String(
                        file.getBytes(),
                        StandardCharsets.UTF_8
                );

        return entrySplitter
                .split(content)
                .stream()
                .map(parsingEngine::parse)
                .map(logAnalysisPipeline::analyze)
                .toList();
    }
    public ErrorGroupingResult group(
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
                        .map(logAnalysisPipeline::analyze)
                        .toList();

        ErrorGroupingResult grouped =
                errorGroupingService.group(
                        analyses
                );

        return severityAnalysisService.analyze(
                grouped
        );
    }
    public List<TraceTimeline> timeline(
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
                        .map(logAnalysisPipeline::analyze)
                        .toList();

        return traceTimelineService
                .buildTimelines(analyses);
    }
    public AnomalyDetectionResult detectAnomalies(
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
                        .map(logAnalysisPipeline::analyze)
                        .toList();

        ErrorGroupingResult grouping =
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

        return anomalyDetectionService.detect(
                analyses,
                grouping,
                timelines
        );
    }
    public RcaAnalysisResult analyzeRca(
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
                        .map(logAnalysisPipeline::analyze)
                        .toList();

        ErrorGroupingResult grouping =
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

        return rcaService.analyze(
                grouping,
                timelines,
                anomalyResult.anomalies()
        );
    }
    }
