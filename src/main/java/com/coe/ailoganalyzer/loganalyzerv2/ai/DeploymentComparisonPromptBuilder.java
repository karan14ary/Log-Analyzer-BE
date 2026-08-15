package com.coe.ailoganalyzer.loganalyzerv2.ai;


import com.coe.ailoganalyzer.loganalyzerv2.model.DeploymentComparison;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeploymentComparisonPromptBuilder {

    private final ObjectMapper objectMapper;



    public String build(
            DeploymentComparison comparison) {

        try {

            String json =
                    objectMapper.writeValueAsString(
                            comparison
                    );

            return """
                    You are a senior Site Reliability Engineer
                    reviewing a production deployment.

                    Analyze the deterministic comparison below.

                    Do not invent information.

                    Explain:

                    1. Whether the deployment improved or
                       degraded system stability.
                    2. The most important regressions.
                    3. The most important improvements.
                    4. Potential deployment-related risks.
                    5. Recommended actions.

                    Return concise technical analysis.

                    DEPLOYMENT COMPARISON:

                    """
                    + json;

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to build deployment comparison prompt",
                    exception
            );
        }
    }
}