package com.coe.ailoganalyzer.loganalyzerv2.ai;




import com.coe.ailoganalyzer.loganalyzerv2.model.DeploymentComparison;
import org.springframework.stereotype.Service;

@Service
public class OllamaDeploymentAnalysisService {

    private final DeploymentComparisonPromptBuilder promptBuilder;

    private final OllamaClient ollamaService;

    public OllamaDeploymentAnalysisService(
            DeploymentComparisonPromptBuilder promptBuilder,
            OllamaClient ollamaService) {

        this.promptBuilder = promptBuilder;
        this.ollamaService = ollamaService;
    }

    public String analyze(
            DeploymentComparison comparison) {

        String prompt =
                promptBuilder.build(comparison);

        return ollamaService.generate(prompt);
    }
}