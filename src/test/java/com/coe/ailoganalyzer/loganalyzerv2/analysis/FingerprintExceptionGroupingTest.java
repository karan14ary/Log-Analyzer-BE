package com.coe.ailoganalyzer.loganalyzerv2.analysis;

import com.coe.ailoganalyzer.loganalyzerv2.exception.ExceptionAnalysisService;
import com.coe.ailoganalyzer.loganalyzerv2.exception.ExceptionAnalyzer;
import com.coe.ailoganalyzer.loganalyzerv2.fingerprint.ExceptionFingerprintNormalizer;
import com.coe.ailoganalyzer.loganalyzerv2.fingerprint.FingerprintNormalizer;
import com.coe.ailoganalyzer.loganalyzerv2.fingerprint.FingerprintService;
import com.coe.ailoganalyzer.loganalyzerv2.grouping.ErrorGroupingService;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroup;
import com.coe.ailoganalyzer.loganalyzerv2.model.ExceptionInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.FingerprintInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEventAnalysis;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import com.coe.ailoganalyzer.loganalyzerv2.model.StackFrame;
import com.coe.ailoganalyzer.loganalyzerv2.model.TraceTimeline;
import com.coe.ailoganalyzer.loganalyzerv2.severity.SeverityDetector;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FingerprintExceptionGroupingTest {

    @Test
    void fingerprintNormalizationReplacesVariableIdentifiers() {
        var normalizer = new FingerprintNormalizer();
        String input = "user 42 at 10.0.0.1 uuid 123e4567-e89b-12d3-a456-426614174000 hex 0xFF";

        assertEquals("user <NUMBER> at <IP> uuid <UUID> hex <HEX>", normalizer.normalize(input));
        assertNull(normalizer.normalize(null));
        assertEquals("  ", normalizer.normalize("  "));
    }

    @Test
    void exceptionFingerprintIncludesOnlyApplicationFrames() {
        var normalizer = new ExceptionFingerprintNormalizer();
        var info = new ExceptionInfo("IllegalStateException", "bad state", List.of(
                new StackFrame("java.lang.Thread", "run", "Thread.java", 1, false),
                new StackFrame("org.springframework.Bean", "get", "Bean.java", 2, false),
                new StackFrame("app.Service", "execute", "Service.java", 3, false),
                new StackFrame(null, "native", null, null, true)
        ), List.of(), "RootException", "root");

        assertEquals("RootException|root|app.Service.execute", normalizer.normalize(info));
        assertEquals("", normalizer.normalize(null));
        assertEquals("", normalizer.normalize(new ExceptionInfo(null, "message", List.of(), List.of(), null, null)));
    }

    @Test
    void fingerprintServiceHashesMessagesAndExceptionsDeterministically() {
        var exceptionNormalizer = new ExceptionFingerprintNormalizer();
        var service = new FingerprintService(new FingerprintNormalizer(), exceptionNormalizer);
        FingerprintInfo first = service.fingerprint("failed request 10");
        FingerprintInfo second = service.fingerprint("failed request 20");

        assertEquals(first.fingerprint(), second.fingerprint());
        assertEquals(first.normalizedMessage(), second.normalizedMessage());
        assertEquals(64, first.fingerprint().length());
        assertEquals("", service.fingerprintException(null).normalizedMessage());
    }

    @Test
    void exceptionAnalyzerExtractsCausesSuppressedFramesAndNativeFrames() {
        var analyzer = new ExceptionAnalyzer();
        String stack = "com.example.TopException: top\n"
                + " at app.Service.run(Service.java:12)\n"
                + " at java.lang.Thread.run(Native Method)\n"
                + "Caused by: java.lang.IllegalArgumentException: root\n"
                + "Suppressed: com.example.CleanupException: cleanup\n"
                + "random line";

        ExceptionInfo info = analyzer.analyze(stack);

        assertEquals("com.example.TopException", info.exceptionType());
        assertEquals("top", info.message());
        assertEquals("java.lang.IllegalArgumentException", info.rootCauseType());
        assertEquals("root", info.rootCauseMessage());
        assertEquals(List.of("com.example.TopException", "java.lang.IllegalArgumentException",
                "com.example.CleanupException"), info.causeChain());
        assertEquals(2, info.stackFrames().size());
        assertEquals(Integer.valueOf(12), info.stackFrames().getFirst().lineNumber());
        assertTrue(info.stackFrames().getLast().nativeMethod());
        assertNull(analyzer.analyze(null).exceptionType());
        assertTrue(analyzer.analyze(" \n").causeChain().isEmpty());
    }

    @Test
    void exceptionAnalysisSkipsUnqualifiedEventsAndAnalyzesErrors() {
        var service = new ExceptionAnalysisService(new ExceptionAnalyzer());
        var infoEvent = event(LogLevel.INFO, "plain info");
        var errorEvent = event(LogLevel.ERROR, "app.FailureException: broken");
        var noLevelEvent = event(null, "app.FailureException: broken");

        assertNull(service.analyze(infoEvent).exceptionInfo().exceptionType());
        assertEquals("app.FailureException", service.analyze(errorEvent).exceptionInfo().exceptionType());
        assertTrue(service.analyze(noLevelEvent).exceptionInfo().causeChain().isEmpty());
        assertEquals("app.SomeError", service.analyze(event(LogLevel.INFO, "app.SomeError: bad"))
                .exceptionInfo().exceptionType());
    }

    @Test
    void groupsAnalysesByFingerprintOrdersGroupsAndTracksTimes() {
        var service = new ErrorGroupingService();
        LogEventAnalysis early = analysis("fp-a", "A", "2025-01-01T00:00:00Z", LogLevel.ERROR);
        LogEventAnalysis later = analysis("fp-a", "A", "2025-01-03T00:00:00Z", LogLevel.ERROR);
        LogEventAnalysis other = analysis("fp-b", "B", "2025-01-02T00:00:00Z", LogLevel.WARN);

        var result = service.group(List.of(early, later, other));

        assertEquals(3, result.totalEvents());
        assertEquals(2, result.uniqueFingerprints());
        assertEquals("fp-a", result.groups().getFirst().fingerprint());
        assertEquals(2, result.groups().getFirst().occurrenceCount());
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), result.groups().getFirst().firstOccurrence());
        assertEquals(Instant.parse("2025-01-03T00:00:00Z"), result.groups().getFirst().lastOccurrence());
        assertEquals(0, service.group(List.of()).totalEvents());
        assertEquals(0, service.group(null).uniqueFingerprints());
    }

    @Test
    void severityDetectorPrioritizesCriticalAndRecognizesLevelsAndText() {
        var detector = new SeverityDetector();
        assertEquals("CRITICAL", detector.detect(group("out of memory")).name());
        assertEquals("ERROR", detector.detect(group("ordinary failure", LogLevel.ERROR)).name());
        assertEquals("ERROR", detector.detect(group("connection refused")).name());
        assertEquals("WARNING", detector.detect(group("ordinary", LogLevel.WARN)).name());
        assertEquals("WARNING", detector.detect(group("retry later")).name());
        assertEquals("INFO", detector.detect(group("ordinary", LogLevel.INFO)).name());
        assertEquals("UNKNOWN", detector.detect(group("ordinary")).name());
        assertEquals("CRITICAL", detector.detect(groupWithException("ordinary", "OutOfMemoryError")).name());
    }

    private static LogEvent event(LogLevel level, String raw) {
        return new LogEvent(null, level, LogFormat.GENERIC, null, null, null, raw,
                null, null, null, raw, Map.of());
    }

    private static LogEventAnalysis analysis(String fingerprint, String message, String timestamp, LogLevel level) {
        var log = new LogEvent(OffsetDateTime.parse(timestamp), level, LogFormat.GENERIC,
                null, null, null, message, null, null, null, message, Map.of());
        return new LogEventAnalysis(log, null, null, new FingerprintInfo(fingerprint, message), null, null);
    }

    private static ErrorGroup group(String message, LogLevel... levels) {
        List<LogEventAnalysis> samples = levels.length == 0
                ? List.of(analysis("fp", message, "2025-01-01T00:00:00Z", null))
                : List.of(analysis("fp", message, "2025-01-01T00:00:00Z", levels[0]));
        return new ErrorGroup("fp", message, samples.size(), Instant.EPOCH, Instant.EPOCH, samples);
    }

    private static ErrorGroup groupWithException(String message, String exceptionType) {
        var sample = new LogEventAnalysis(
                event(LogLevel.ERROR, message),
                new ExceptionInfo(exceptionType, "", List.of(), List.of(), exceptionType, ""),
                null, new FingerprintInfo("fp", message), null, null);
        return new ErrorGroup("fp", message, 1, Instant.EPOCH, Instant.EPOCH, List.of(sample));
    }
}
