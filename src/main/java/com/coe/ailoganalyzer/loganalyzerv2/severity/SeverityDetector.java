package com.coe.ailoganalyzer.loganalyzerv2.severity;

import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroup;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import com.coe.ailoganalyzer.loganalyzerv2.model.Severity;
import org.springframework.stereotype.Component;




@Component
public class SeverityDetector {

    public Severity detect(
            ErrorGroup group) {

        if (hasCriticalException(group)) {
            return Severity.CRITICAL;
        }

        String message =
                group.normalizedMessage()
                        .toLowerCase();

        if (containsCriticalPattern(message)) {
            return Severity.CRITICAL;
        }

        if (hasErrorLogLevel(group)) {
            return Severity.ERROR;
        }

        if (containsErrorPattern(message)) {
            return Severity.ERROR;
        }

        if (hasWarningLogLevel(group)) {
            return Severity.WARNING;
        }

        if (containsWarningPattern(message)) {
            return Severity.WARNING;
        }

        if (hasInfoLogLevel(group)) {
            return Severity.INFO;
        }

        return Severity.UNKNOWN;
    }

    private boolean hasCriticalException(
            ErrorGroup group) {

        return group.samples()
                .stream()
                .map(LogEventAnalysis::exceptionInfo)
                .filter(exception -> exception != null)
                .anyMatch(exception ->
                        exception.exceptionType() != null
                                && (
                                exception.exceptionType()
                                        .contains("OutOfMemoryError")
                                        || exception.exceptionType()
                                        .contains("StackOverflowError")
                        )
                );
    }

    private boolean hasErrorLogLevel(
            ErrorGroup group) {

        return group.samples()
                .stream()
                .anyMatch(event ->
                        "ERROR".equalsIgnoreCase(
                                String.valueOf(event.event().level())
                        )
                );
    }

    private boolean hasWarningLogLevel(
            ErrorGroup group) {

        return group.samples()
                .stream()
                .anyMatch(event ->
                        "WARN".equalsIgnoreCase(
                                String.valueOf(event.event().level())
                        )
                                || "WARNING".equalsIgnoreCase(
                                String.valueOf(event.event().level())
                        )
                );
    }

    private boolean hasInfoLogLevel(
            ErrorGroup group) {

        return group.samples()
                .stream()
                .anyMatch(event ->
                        "INFO".equalsIgnoreCase(
                                String.valueOf(event.event().level())
                        )
                );
    }

    private boolean containsCriticalPattern(
            String message) {

        return message.contains("outofmemoryerror")
                || message.contains("out of memory")
                || message.contains("stackoverflowerror")
                || message.contains("fatal")
                || message.contains("system failure")
                || message.contains("database unavailable")
                || message.contains("service unavailable");
    }

    private boolean containsErrorPattern(
            String message) {

        return message.contains("exception")
                || message.contains("failed")
                || message.contains("failure")
                || message.contains("error")
                || message.contains("timeout")
                || message.contains("connection refused");
    }

    private boolean containsWarningPattern(
            String message) {

        return message.contains("warning")
                || message.contains("deprecated")
                || message.contains("retry")
                || message.contains("slow")
                || message.contains("high usage");
    }
}