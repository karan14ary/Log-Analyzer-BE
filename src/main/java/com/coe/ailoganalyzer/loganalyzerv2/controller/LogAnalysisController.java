package com.coe.ailoganalyzer.loganalyzerv2.controller;

import com.coe.ailoganalyzer.loganalyzerv2.ai.RcaService;
import com.coe.ailoganalyzer.loganalyzerv2.model.*;
import com.coe.ailoganalyzer.loganalyzerv2.report.IncidentMarkdownRenderer;
import com.coe.ailoganalyzer.loganalyzerv2.report.IncidentReportService;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContextService;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisPipeline;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogAnalysisController {

    private final LogAnalysisService service;
    private final IncidentReportService incidentReportService;
    private final RcaService rcaService;
    private final LogAnalysisContextService contextService;
    private final IncidentMarkdownRenderer incidentMarkdownRenderer;
    @PostMapping(
            value = "/parse",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<LogEvent> parse(
            @RequestParam("file")
            MultipartFile file) throws Exception {

        return service.parse(file);
    }
    @PostMapping(
            value = "/analyze-exceptions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<LogEventAnalysis> analyzeExceptions(
            @RequestParam("file") MultipartFile file)
            throws Exception {

        return service.analyzeExceptions(file);
    }
    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<LogEventAnalysis> analyze(
            @RequestParam("file") MultipartFile file)
            throws Exception {

        return service.analyze(file);
    }
    @PostMapping(
            value = "/group",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ErrorGroupingResult group(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        return service
                .group(file);
    }
    @PostMapping(
            value = "/timeline",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<TraceTimeline> timeline(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        return service.timeline(file);
    }
    @PostMapping(
            value = "/anomalies",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public AnomalyDetectionResult anomalies(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        return service.detectAnomalies(
                file
        );
    }
    @PostMapping(
            value = "/rca",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public RcaAnalysisResult rca(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        return service.analyzeRca(
                file
        );
    }
    @PostMapping(
            value = "/incident-report",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public IncidentReport incidentReport(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        LogAnalysisContext context =
                contextService.analyze(file);

        RcaAnalysisResult rca =
                rcaService.analyze(context);

        return incidentReportService.generate(
                context,
                rca
        );
    }
    @PostMapping(
            value = "/incident-report/markdown",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String incidentReportMarkdown(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        LogAnalysisContext context =
                contextService.analyze(file);

        RcaAnalysisResult rca =
                rcaService.analyze(context);

        IncidentReport report =
                incidentReportService.generate(
                        context,
                        rca
                );

        return incidentMarkdownRenderer.render(
                report
        );
    }
}