package com.coe.ailoganalyzer.loganalyzerv2.ai;

public record OllamaGenerateResponse(

        String model,

        String response,

        boolean done

) {
}