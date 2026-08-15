package com.coe.ailoganalyzer.loganalyzerv2.model;


import java.util.List;

public record ErrorComparison(

        List<String> newErrors,

        List<String> resolvedErrors,

        List<String> persistentErrors,

        List<String> increasedErrors,

        List<String> decreasedErrors

) {
}