package com.coe.ailoganalyzer.loganalyzerv2.ai;

import com.coe.ailoganalyzer.loganalyzerv2.model.Anomaly;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroupingResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.RcaResult;
import com.coe.ailoganalyzer.loganalyzerv2.model.TraceTimeline;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RcaServiceTest {

    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private RcaContextBuilder contextBuilder;
    @Mock
    private RcaPromptBuilder promptBuilder;

    @Test
    void parsesJsonModeResponseAndIgnoresMarkdownJsonFence() {
        var service = new RcaService(ollamaClient, contextBuilder, promptBuilder, new ObjectMapper());
        var grouping = new ErrorGroupingResult(0, 0, List.of());
        when(contextBuilder.build(grouping, List.of(), List.of())).thenReturn("evidence");
        when(promptBuilder.build("evidence")).thenReturn("prompt");
        when(ollamaClient.generate("prompt")).thenReturn("```json\n{\"type\":\"object\",\"rootCause\":\"pool exhausted\","
                + "\"confidence\":0.9,\"impact\":\"errors\",\"evidence\":[],\"recommendations\":[]}\n```");

        var result = service.analyze(grouping, List.of(), List.of());

        assertTrue(result.successful());
        assertEquals("pool exhausted", result.result().rootCause());
        assertEquals(0.9, result.result().confidence(), 0.0);
    }

    @Test
    void parsesGenericMarkdownFenceForContextOverload() {
        var service = new RcaService(ollamaClient, contextBuilder, promptBuilder, new ObjectMapper());
        var context = new LogAnalysisContext(List.of(), new ErrorGroupingResult(0, 0, List.of()), List.of(), List.of());
        when(contextBuilder.build(context.grouping(), context.timelines(), context.anomalies())).thenReturn("evidence");
        when(promptBuilder.build("evidence")).thenReturn("prompt");
        when(ollamaClient.generate("prompt")).thenReturn("```{\"rootCause\":\"unknown\",\"confidence\":0.1,"
            + "\"impact\":\"none\",\"evidence\":[],\"recommendations\":[]} ```");

        var result = service.analyze(context);

        assertTrue(result.successful());
        assertEquals("unknown", result.result().rootCause());
    }

    @Test
    void returnsFailureWhenModelResponseIsNotJson() {
        var service = new RcaService(ollamaClient, contextBuilder, promptBuilder, new ObjectMapper());
        var grouping = new ErrorGroupingResult(0, 0, List.of());
        when(contextBuilder.build(grouping, List.of(), List.of())).thenReturn("evidence");
        when(promptBuilder.build("evidence")).thenReturn("prompt");
        when(ollamaClient.generate("prompt")).thenReturn("It could not determine the root cause");

        var result = service.analyze(grouping, List.of(), List.of());

        assertFalse(result.successful());
        assertTrue(result.error().contains("Unrecognized token"));
    }
}
