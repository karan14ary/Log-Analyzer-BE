package com.coe.ailoganalyzer.loganalyzerv2.exception;

import com.coe.ailoganalyzer.loganalyzerv2.model.ExceptionInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExceptionAnalysisService {

    private final ExceptionAnalyzer exceptionAnalyzer;

    public LogEventAnalysis analyze(
            LogEvent event) {

        if (event.level() == null
                || event.rawLog() == null) {

            return new LogEventAnalysis(
                    event,
                    emptyExceptionInfo(),
                    null,
                    null,
                    null,
                    null

            );
        }

        boolean potentiallyException =
                event.level().name().equals("ERROR")
                        || event.rawLog()
                        .contains("Exception")
                        || event.rawLog()
                        .contains("Error");

        if (!potentiallyException) {

            return new LogEventAnalysis(
                    event,
                    emptyExceptionInfo(),
                    null,
                    null,
                    null,
                    null
            );
        }

        ExceptionInfo info =
                exceptionAnalyzer.analyze(
                        event.rawLog()
                );

        return new LogEventAnalysis(
                event,
                info,
                null,
                null,
                null,
                null
        );
    }

    private ExceptionInfo emptyExceptionInfo() {

        return new ExceptionInfo(
                null,
                null,
                java.util.List.of(),
                java.util.List.of(),
                null,
                null
        );
    }
}