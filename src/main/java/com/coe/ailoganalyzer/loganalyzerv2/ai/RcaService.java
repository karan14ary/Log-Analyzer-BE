package com.coe.ailoganalyzer.loganalyzerv2.ai;

import com.coe.ailoganalyzer.loganalyzerv2.model.*;
import com.coe.ailoganalyzer.loganalyzerv2.service.LogAnalysisContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RcaService {

    private final OllamaClient ollamaClient;

    private final RcaContextBuilder contextBuilder;

    private final RcaPromptBuilder promptBuilder;

    private final ObjectMapper objectMapper;

    public RcaService(
            OllamaClient ollamaClient,
            RcaContextBuilder contextBuilder,
            RcaPromptBuilder promptBuilder,
            ObjectMapper objectMapper) {

        this.ollamaClient = ollamaClient;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }
    public RcaAnalysisResult analyze(
            LogAnalysisContext context) {

        try {

            String evidence =
                    contextBuilder.build(
                            context.grouping(),
                            context.timelines(),
                            context.anomalies()
                    );

            String prompt =
                    promptBuilder.build(
                            evidence
                    );

            String response =
                    ollamaClient.generate(
                            prompt
                    );

            response = cleanJsonResponse(response);

            RcaResult result =
                    objectMapper.readValue(
                            response,
                            RcaResult.class
                    );

            return new RcaAnalysisResult(
                    true,
                    result,
                    null
            );

        } catch (Exception exception) {

            return new RcaAnalysisResult(
                    false,
                    null,
                    exception.getMessage()
            );
        }
    }
    public RcaAnalysisResult analyze(
            ErrorGroupingResult grouping,
            List<TraceTimeline> timelines,
            List<Anomaly> anomalies) {

        try {

            String context =
                    contextBuilder.build(
                            grouping,
                            timelines,
                            anomalies
                    );

            String prompt =
                    promptBuilder.build(
                            context
                    );

            String response =
                    ollamaClient.generate(
                            prompt
                    );

            response = cleanJsonResponse(response);

            RcaResult result =
                    objectMapper.readValue(
                            response,
                            RcaResult.class
                    );

            return new RcaAnalysisResult(
                    true,
                    result,
                    null
            );

        } catch (Exception exception) {

            return new RcaAnalysisResult(
                    false,
                    null,
                    exception.getMessage()
            );
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null) return null;
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}