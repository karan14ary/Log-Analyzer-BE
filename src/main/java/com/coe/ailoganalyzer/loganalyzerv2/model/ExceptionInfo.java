package com.coe.ailoganalyzer.loganalyzerv2.model;


import java.util.List;

public record ExceptionInfo(

        String exceptionType,

        String message,

        List<StackFrame> stackFrames,

        List<String> causeChain,

        String rootCauseType,

        String rootCauseMessage

) {
}