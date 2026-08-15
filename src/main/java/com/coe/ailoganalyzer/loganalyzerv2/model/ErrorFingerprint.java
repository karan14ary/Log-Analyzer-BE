package com.coe.ailoganalyzer.loganalyzerv2.model;


public record ErrorFingerprint(

        String fingerprint,

        String exceptionType,

        String normalizedMessage

) {
}