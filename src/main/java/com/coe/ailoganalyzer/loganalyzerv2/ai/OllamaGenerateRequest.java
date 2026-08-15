package com.coe.ailoganalyzer.loganalyzerv2.ai;


public record OllamaGenerateRequest(

        String model,

        String prompt,

        boolean stream,

        double temperature

) {
}