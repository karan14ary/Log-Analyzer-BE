package com.coe.ailoganalyzer.loganalyzerv2.fingerprint;


import com.coe.ailoganalyzer.loganalyzerv2.model.ExceptionInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.StackFrame;
import org.springframework.stereotype.Component;

@Component
public class ExceptionFingerprintNormalizer {

    public String normalize(
            ExceptionInfo exceptionInfo) {

        if (exceptionInfo == null
                || exceptionInfo.exceptionType() == null) {

            return "";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                exceptionInfo.rootCauseType()
        );

        result.append("|");

        result.append(
                exceptionInfo.rootCauseMessage()
        );

        for (StackFrame frame :
                exceptionInfo.stackFrames()) {

            if (isApplicationFrame(frame)) {

                result.append("|")
                        .append(frame.className())
                        .append(".")
                        .append(frame.methodName());
            }
        }

        return result.toString();
    }

    private boolean isApplicationFrame(
            StackFrame frame) {

        String className =
                frame.className();

        if (className == null) {
            return false;
        }

        return !className.startsWith("java.")
                && !className.startsWith("jdk.")
                && !className.startsWith("sun.")
                && !className.startsWith("org.springframework.");
    }
}