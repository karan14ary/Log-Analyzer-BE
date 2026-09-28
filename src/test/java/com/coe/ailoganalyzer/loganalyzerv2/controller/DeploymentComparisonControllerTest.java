package com.coe.ailoganalyzer.loganalyzerv2.controller;

import com.coe.ailoganalyzer.loganalyzerv2.comparision.DeploymentComparisonService;
import com.coe.ailoganalyzer.loganalyzerv2.model.DeploymentComparison;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContextService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeploymentComparisonControllerTest {

    @Test
    void analyzesBothFilesAndComparesTheirContexts() throws IOException {
        var contextService = mock(LogAnalysisContextService.class);
        var comparisonService = mock(DeploymentComparisonService.class);
        var controller = new DeploymentComparisonController(contextService, comparisonService);
        var beforeFile = mock(MultipartFile.class);
        var afterFile = mock(MultipartFile.class);
        var before = new LogAnalysisContext(List.of(), null, List.of(), List.of());
        var after = new LogAnalysisContext(List.of(), null, List.of(), List.of());
        var comparison = mock(DeploymentComparison.class);
        when(contextService.analyze(beforeFile)).thenReturn(before);
        when(contextService.analyze(afterFile)).thenReturn(after);
        when(comparisonService.compare(before, after)).thenReturn(comparison);

        assertEquals(comparison, controller.compare(beforeFile, afterFile));
        verify(contextService).analyze(beforeFile);
        verify(contextService).analyze(afterFile);
        verify(comparisonService).compare(before, after);
    }
}
