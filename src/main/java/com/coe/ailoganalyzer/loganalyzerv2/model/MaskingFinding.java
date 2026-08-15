package com.coe.ailoganalyzer.loganalyzerv2.model;

public record MaskingFinding(

        MaskingType type,

        String replacement,

        int occurrence

) {
}