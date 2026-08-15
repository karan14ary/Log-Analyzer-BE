package com.coe.ailoganalyzer.loganalyzerv2.ai;


import org.springframework.stereotype.Component;

@Component
public class RcaPromptBuilder {

    public String build(
            String context) {

        return """
                You are an expert Site Reliability Engineer
                and production incident investigator.

                Analyze the provided application log evidence.

                Your task is to determine the most likely
                root cause of the incident.

                IMPORTANT RULES:

                1. Use only the evidence provided.
                2. Do not invent services, infrastructure,
                   errors, or events.
                3. Distinguish evidence from inference.
                4. If the evidence is insufficient,
                   say so.
                5. Do not claim certainty when the evidence
                   only supports a hypothesis.
                6. Return ONLY valid JSON.
                7. confidence must be between 0 and 1.

                Return exactly this JSON structure:

                {
                  "rootCause": "...",
                  "confidence": 0.0,
                  "impact": "...",
                  "evidence": [
                    "...",
                    "..."
                  ],
                  "recommendations": [
                    "...",
                    "..."
                  ]
                }

                LOG ANALYSIS:

                """
                + context;
    }
}