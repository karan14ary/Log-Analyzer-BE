package com.coe.ailoganalyzer.loganalyzerv2.service;

import com.coe.ailoganalyzer.loganalyzerv2.ai.RcaService;
import com.coe.ailoganalyzer.loganalyzerv2.anomaly.AnomalyDetectionService;
import com.coe.ailoganalyzer.loganalyzerv2.exception.ExceptionAnalysisService;
import com.coe.ailoganalyzer.loganalyzerv2.grouping.ErrorGroupingService;
import com.coe.ailoganalyzer.loganalyzerv2.model.Anomaly;
import com.coe.ailoganalyzer.loganalyzerv2.model.AnomalyDetectionResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroupingResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import com.coe.ailoganalyzer.loganalyzerv2.model.RcaAnalysisResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.TraceTimeline;
import com.coe.ailoganalyzer.loganalyzerv2.parser.LogEntrySplitter;
import com.coe.ailoganalyzer.loganalyzerv2.parser.strategies.LogParsingEngine;
import com.coe.ailoganalyzer.loganalyzerv2.severity.SeverityAnalysisService;
import com.coe.ailoganalyzer.loganalyzerv2.trace.TraceTimelineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogAnalysisServiceTest {

    @Mock private LogEntrySplitter entrySplitter;
    @Mock private LogParsingEngine parsingEngine;
    @Mock private ExceptionAnalysisService exceptionAnalysisService;
    @Mock private LogAnalysisPipeline logAnalysisPipeline;
    @Mock private ErrorGroupingService errorGroupingService;
    @Mock private SeverityAnalysisService severityAnalysisService;
    @Mock private TraceTimelineService traceTimelineService;
    @Mock private AnomalyDetectionService anomalyDetectionService;
    @Mock private RcaService rcaService;
    @Mock private MultipartFile file;
    @InjectMocks private LogAnalysisService service;

    @Test
    void publicOperationsParseAndDelegateTheUploadedLog() throws IOException {
        var event = org.mockito.Mockito.mock(LogEvent.class);
        var analysis = org.mockito.Mockito.mock(LogEventAnalysis.class);
        var grouping = new ErrorGroupingResult(1, 1, List.of());
        var timeline = org.mockito.Mockito.mock(TraceTimeline.class);
        var anomaly = org.mockito.Mockito.mock(Anomaly.class);
        var anomalyResult = new AnomalyDetectionResult(1, 1, 1, 1, List.of(anomaly));
        var rcaResult = new RcaAnalysisResult(false, null, "unavailable");
        List<String> entries = List.of("log line");
        List<TraceTimeline> timelines = List.of(timeline);
        List<Anomaly> anomalies = List.of(anomaly);

        when(file.getBytes()).thenReturn("log line".getBytes());
        when(entrySplitter.split("log line")).thenReturn(entries);
        when(parsingEngine.parse("log line")).thenReturn(event);
        when(exceptionAnalysisService.analyze(event)).thenReturn(analysis);
        when(logAnalysisPipeline.analyze(event)).thenReturn(analysis);
        when(errorGroupingService.group(List.of(analysis))).thenReturn(grouping);
        when(severityAnalysisService.analyze(grouping)).thenReturn(grouping);
        when(traceTimelineService.buildTimelines(List.of(analysis))).thenReturn(timelines);
        when(anomalyDetectionService.detect(List.of(analysis), grouping, timelines)).thenReturn(anomalyResult);
        when(rcaService.analyze(grouping, timelines, anomalies)).thenReturn(rcaResult);

        assertEquals(List.of(event), service.parse(file));
        assertEquals(List.of(analysis), service.analyzeExceptions(file));
        assertEquals(List.of(analysis), service.analyze(file));
        assertEquals(grouping, service.group(file));
        assertEquals(timelines, service.timeline(file));
        assertEquals(anomalyResult, service.detectAnomalies(file));
        assertEquals(rcaResult, service.analyzeRca(file));
    }
}
