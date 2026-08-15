package com.coe.ailoganalyzer.loganalyzerv2.model;

import java.util.List;

public record MaskingResult(

        String maskedText,

        List<MaskingFinding> findings

) {
}