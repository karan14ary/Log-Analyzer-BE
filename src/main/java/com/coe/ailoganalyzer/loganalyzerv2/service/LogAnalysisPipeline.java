package com.coe.ailoganalyzer.loganalyzerv2.service;

import com.coe.ailoganalyzer.loganalyzerv2.exception.ExceptionAnalyzer;
import com.coe.ailoganalyzer.loganalyzerv2.fingerprint.FingerprintService;
import com.coe.ailoganalyzer.loganalyzerv2.grouping.ErrorGroupingService;
import com.coe.ailoganalyzer.loganalyzerv2.masking.LogMaskingService;
import com.coe.ailoganalyzer.loganalyzerv2.model.*;
import com.coe.ailoganalyzer.loganalyzerv2.trace.TraceIdExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogAnalysisPipeline {

    private final ExceptionAnalyzer exceptionAnalyzer;

    private final LogMaskingService maskingService;

    private final FingerprintService fingerprintService;

    private final TraceIdExtractor traceIdExtractor;
    public LogEventAnalysis analyze(
            LogEvent event) {

        ExceptionInfo exceptionInfo =
                exceptionAnalyzer.analyze(
                        event.rawLog()
                );

        MaskingResult maskingResult =
                maskingService.mask(event);
        String traceId =
                traceIdExtractor.extractTraceId(
                        event.rawLog()
                );

        String correlationId =
                traceIdExtractor.extractCorrelationId(
                        event.rawLog()
                );

        FingerprintInfo fingerprintInfo;

        if (exceptionInfo.exceptionType() != null) {

            fingerprintInfo =
                    fingerprintService
                            .fingerprintException(
                                    exceptionInfo
                            );

        } else {

            fingerprintInfo =
                    fingerprintService
                            .fingerprint(
                                    event.message()
                            );
        }

        return new LogEventAnalysis(
                event,
                exceptionInfo,
                maskingResult,
                fingerprintInfo,
                traceId,
                correlationId
        );
    }
}