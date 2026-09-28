package com.coe.ailoganalyzer.loganalyzerv2.ai;


public record OllamaGenerateRequest(

        String model,

        String prompt,

        boolean stream,

                String format,

                Options options

) {

        public record Options(double temperature) {
        }
}