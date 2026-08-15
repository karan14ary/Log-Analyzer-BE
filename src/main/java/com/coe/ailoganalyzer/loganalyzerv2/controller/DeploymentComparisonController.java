package com.coe.ailoganalyzer.loganalyzerv2.controller;

import com.coe.ailoganalyzer.loganalyzerv2.comparision.DeploymentComparisonService;
import com.coe.ailoganalyzer.loganalyzerv2.model.DeploymentComparison;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContextService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/logs")
public class DeploymentComparisonController {

    private final LogAnalysisContextService contextService;

    private final DeploymentComparisonService comparisonService;

    public DeploymentComparisonController(
            LogAnalysisContextService contextService,
            DeploymentComparisonService comparisonService) {

        this.contextService =
                contextService;

        this.comparisonService =
                comparisonService;
    }

    @PostMapping(
            value = "/deployment-comparison",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public DeploymentComparison compare(
            @RequestParam("before")
            MultipartFile before,

            @RequestParam("after")
            MultipartFile after)
            throws IOException {

        LogAnalysisContext beforeContext =
                contextService.analyze(
                        before
                );

        LogAnalysisContext afterContext =
                contextService.analyze(
                        after
                );

        return comparisonService.compare(
                beforeContext,
                afterContext
        );
    }
}