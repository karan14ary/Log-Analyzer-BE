package com.coe.ailoganalyzer.loganalyzerv2.ai;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama")
public record OllamaConfig(

        String baseUrl,

        String model,

        double temperature

) {
}