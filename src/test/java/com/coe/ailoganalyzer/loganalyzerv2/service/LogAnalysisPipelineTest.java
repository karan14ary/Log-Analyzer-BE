package com.coe.ailoganalyzer.loganalyzerv2.service;

import com.coe.ailoganalyzer.loganalyzerv2.exception.ExceptionAnalyzer;
import com.coe.ailoganalyzer.loganalyzerv2.fingerprint.FingerprintService;
import com.coe.ailoganalyzer.loganalyzerv2.masking.LogMaskingService;
import com.coe.ailoganalyzer.loganalyzerv2.model.ExceptionInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.FingerprintInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingResult;
import com.coe.ailoganalyzer.loganalyzerv2.trace.TraceIdExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogAnalysisPipelineTest {

    @Mock
    private ExceptionAnalyzer exceptionAnalyzer;
    @Mock
    private LogMaskingService maskingService;
    @Mock
    private FingerprintService fingerprintService;
    @Mock
    private TraceIdExtractor traceIdExtractor;
    @InjectMocks
    private LogAnalysisPipeline pipeline;

    @Test
    void usesExceptionFingerprintWhenExceptionWasFound() {
        var event = event("java.lang.IllegalStateException: failed", "failed");
        var exception = new ExceptionInfo("java.lang.IllegalStateException", "failed",
                List.of(), List.of(), "java.lang.IllegalStateException", "failed");
        var masking = new MaskingResult("redacted", List.of());
        var fingerprint = new FingerprintInfo("exception-fp", "normalized exception");
        when(exceptionAnalyzer.analyze(event.rawLog())).thenReturn(exception);
        when(maskingService.mask(event)).thenReturn(masking);
        when(traceIdExtractor.extractTraceId(event.rawLog())).thenReturn("trace-1");
        when(traceIdExtractor.extractCorrelationId(event.rawLog())).thenReturn("corr-1");
        when(fingerprintService.fingerprintException(exception)).thenReturn(fingerprint);

        var result = pipeline.analyze(event);

        assertEquals(exception, result.exceptionInfo());
        assertEquals(masking, result.maskingResult());
        assertEquals(fingerprint, result.fingerprintInfo());
        assertEquals("trace-1", result.traceId());
        assertEquals("corr-1", result.correlationId());
        verify(fingerprintService).fingerprintException(exception);
    }

    @Test
    void fingerprintsTheMessageWhenNoExceptionWasFound() {
        var event = event("ordinary log", "ordinary message");
        var fingerprint = new FingerprintInfo("message-fp", "ordinary message");
        when(exceptionAnalyzer.analyze(event.rawLog())).thenReturn(
                new ExceptionInfo(null, null, List.of(), List.of(), null, null));
        when(maskingService.mask(event)).thenReturn(new MaskingResult(event.rawLog(), List.of()));
        when(traceIdExtractor.extractTraceId(event.rawLog())).thenReturn(null);
        when(traceIdExtractor.extractCorrelationId(event.rawLog())).thenReturn(null);
        when(fingerprintService.fingerprint(event.message())).thenReturn(fingerprint);

        var result = pipeline.analyze(event);

        assertEquals(fingerprint, result.fingerprintInfo());
        verify(fingerprintService).fingerprint(event.message());
    }

    private static LogEvent event(String rawLog, String message) {
        return new LogEvent(null, LogLevel.ERROR, LogFormat.GENERIC, "app", null, null,
                message, null, null, null, rawLog, Map.of());
    }
}
