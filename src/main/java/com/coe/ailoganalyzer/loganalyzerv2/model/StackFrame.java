package com.coe.ailoganalyzer.loganalyzerv2.model;


public record StackFrame(
        String className,
        String methodName,
        String fileName,
        Integer lineNumber,
        boolean nativeMethod
) {
}