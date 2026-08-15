package com.coe.ailoganalyzer.loganalyzerv2.ai;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OllamaHealthService {

    private final RestClient client;

    public OllamaHealthService(
            RestClient.Builder builder,
            OllamaConfig config) {

        this.client =
                builder
                        .baseUrl(config.baseUrl())
                        .build();
    }

    public boolean isAvailable() {

        try {

            client.get()
                    .uri("/")
                    .retrieve()
                    .toBodilessEntity();

            return true;

        } catch (Exception exception) {

            return false;
        }
    }
}
