package com.coe.ailoganalyzer.loganalyzerv2.controller;

import com.coe.ailoganalyzer.loganalyzerv2.ai.RcaService;
import com.coe.ailoganalyzer.loganalyzerv2.model.AnomalyDetectionResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroupingResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.IncidentReport;
import com.coe.ailoganalyzer.loganalyzerv2.model.RcaAnalysisResult;
import com.coe.ailoganalyzer.loganalyzerv2.report.IncidentMarkdownRenderer;
import com.coe.ailoganalyzer.loganalyzerv2.report.IncidentReportService;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContextService;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogAnalysisControllerTest {

    @Test
    void endpointsDelegateAndBuildIncidentOutputs() throws Exception {
        var service = mock(LogAnalysisService.class);
        var reportService = mock(IncidentReportService.class);
        var rcaService = mock(RcaService.class);
        var contextService = mock(LogAnalysisContextService.class);
        var renderer = mock(IncidentMarkdownRenderer.class);
        var controller = new LogAnalysisController(service, reportService, rcaService, contextService, renderer);
        var file = mock(MultipartFile.class);
        var context = new LogAnalysisContext(List.of(), new ErrorGroupingResult(0, 0, List.of()), List.of(), List.of());
        var rca = new RcaAnalysisResult(false, null, "unavailable");
        var report = new IncidentReport("INC-1", "LOW", null, "summary", Instant.EPOCH, Instant.EPOCH,
                Duration.ZERO, 0, 0, 0, 0, List.of(), List.of(), rca, List.of());
        var grouping = new ErrorGroupingResult(0, 0, List.of());
        var anomalies = new AnomalyDetectionResult(0, 0, 0, 0, List.of());

        when(service.parse(file)).thenReturn(List.of());
        when(service.analyzeExceptions(file)).thenReturn(List.of());
        when(service.analyze(file)).thenReturn(List.of());
        when(service.group(file)).thenReturn(grouping);
        when(service.timeline(file)).thenReturn(List.of());
        when(service.detectAnomalies(file)).thenReturn(anomalies);
        when(service.analyzeRca(file)).thenReturn(rca);
        when(contextService.analyze(file)).thenReturn(context);
        when(rcaService.analyze(context)).thenReturn(rca);
        when(reportService.generate(context, rca)).thenReturn(report);
        when(renderer.render(report)).thenReturn("# report");

        assertEquals(List.of(), controller.parse(file));
        assertEquals(List.of(), controller.analyzeExceptions(file));
        assertEquals(List.of(), controller.analyze(file));
        assertEquals(grouping, controller.group(file));
        assertEquals(List.of(), controller.timeline(file));
        assertEquals(anomalies, controller.anomalies(file));
        assertEquals(rca, controller.rca(file));
        assertEquals(report, controller.incidentReport(file));
        assertEquals("# report", controller.incidentReportMarkdown(file));

        verify(contextService, times(2)).analyze(file);
        verify(rcaService, times(2)).analyze(context);
        verify(renderer).render(report);
    }
}
