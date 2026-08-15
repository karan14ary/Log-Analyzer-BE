package com.coe.ailoganalyzer.loganalyzerv2.ai;


import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaClient {

    private final RestClient restClient;

    private final OllamaConfig config;

    public OllamaClient(
            RestClient.Builder builder,
            OllamaConfig config) {

        this.config = config;

        this.restClient =
                builder
                        .baseUrl(config.baseUrl())
                        .build();
    }

    public String generate(
            String prompt) {

        OllamaGenerateRequest request =
                new OllamaGenerateRequest(
                        config.model(),
                        prompt,
                        false,
                        config.temperature()
                );

        OllamaGenerateResponse response =
                restClient
                        .post()
                        .uri("/api/generate")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .body(request)
                        .retrieve()
                        .body(
                                OllamaGenerateResponse.class
                        );

        if (response == null
                || response.response() == null) {

            throw new IllegalStateException(
                    "Ollama returned an empty response"
            );
        }

        return response.response();
    }
}