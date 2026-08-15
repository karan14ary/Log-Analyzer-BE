package com.coe.ailoganalyzer.loganalyzerv2.severity;

import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroup;
import com.coe.ailoganalyzer.loganalyzerv2.model.ErrorGroupingResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeverityAnalysisService {

    private final SeverityDetector severityDetector;

    public SeverityAnalysisService(
            SeverityDetector severityDetector) {

        this.severityDetector =
                severityDetector;
    }

    public ErrorGroupingResult analyze(
            ErrorGroupingResult result) {

        List<ErrorGroup> groups =
                result.groups()
                        .stream()
                        .map(this::addSeverity)
                        .toList();

        return new ErrorGroupingResult(
                result.totalEvents(),
                result.uniqueFingerprints(),
                groups
        );
    }

    private ErrorGroup addSeverity(
            ErrorGroup group) {

        var severity =
                severityDetector.detect(group);

        return new ErrorGroup(
                group.fingerprint(),
                group.normalizedMessage(),
                group.occurrenceCount(),
                group.firstOccurrence(),
                group.lastOccurrence(),
                group.samples()
        );
    }
}