package com.coe.ailoganalyzer.loganalyzerv2.ai;


import com.coe.ailoganalyzer.loganalyzerv2.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RcaContextBuilder {

    public String build(
            ErrorGroupingResult grouping,
            List<TraceTimeline> timelines,
            List<Anomaly> anomalies) {

        StringBuilder context =
                new StringBuilder();

        context.append("""
                LOG ANALYSIS CONTEXT

                Analyze the following structured
                application log evidence.

                Do not invent facts.
                Base conclusions only on the evidence provided.

                """);

        appendGroups(
                context,
                grouping
        );

        appendTimelines(
                context,
                timelines
        );

        appendAnomalies(
                context,
                anomalies
        );

        return context.toString();
    }

    private void appendGroups(
            StringBuilder context,
            ErrorGroupingResult grouping) {

        context.append(
                "\nERROR GROUPS:\n"
        );

        for (ErrorGroup group :
                grouping.groups()) {

            context.append(
                    "\nFingerprint: "
            ).append(
                    group.fingerprint()
            );

            context.append(
                    "\nMessage: "
            ).append(
                    group.normalizedMessage()
            );

            context.append(
                    "\nOccurrences: "
            ).append(
                    group.occurrenceCount()
            );


            context.append(
                    "\nFirst occurrence: "
            ).append(
                    group.firstOccurrence()
            );

            context.append(
                    "\nLast occurrence: "
            ).append(
                    group.lastOccurrence()
            );

            appendSamples(
                    context,
                    group.samples()
            );

            context.append("\n");
        }
    }

    private void appendSamples(
            StringBuilder context,
            List<LogEventAnalysis> samples) {

        context.append(
                "\nSamples:\n"
        );

        for (LogEventAnalysis analysis :
                samples) {

            context.append(
                    "- "
            ).append(
                    analysis.event().message()
            );

            if (analysis.exceptionInfo() != null) {

                context.append(
                        " | Exception: "
                ).append(
                        analysis.exceptionInfo()
                                .exceptionType()
                );
            }

            context.append("\n");
        }
    }

    private void appendTimelines(
            StringBuilder context,
            List<TraceTimeline> timelines) {

        context.append(
                "\nTRACE TIMELINES:\n"
        );

        for (TraceTimeline timeline :
                timelines) {

            context.append(
                    "\nTrace ID: "
            ).append(
                    timeline.traceId()
            );

            context.append(
                    "\nDuration: "
            ).append(
                    timeline.duration()
            );

            context.append(
                    "\nEvents: "
            ).append(
                    timeline.eventCount()
            );

            context.append(
                    "\nErrors: "
            ).append(
                    timeline.errorCount()
            );

            context.append(
                    "\nRoot cause candidate: "
            ).append(
                    timeline.rootCauseFingerprint()
            );

            context.append("\n");
        }
    }

    private void appendAnomalies(
            StringBuilder context,
            List<Anomaly> anomalies) {

        context.append(
                "\nANOMALIES:\n"
        );

        for (Anomaly anomaly :
                anomalies) {

            context.append(
                    "- Type: "
            ).append(
                    anomaly.type()
            );

            context.append(
                    ", Severity: "
            ).append(
                    anomaly.severity()
            );

            context.append(
                    ", Score: "
            ).append(
                    anomaly.score()
            );

            context.append(
                    ", Description: "
            ).append(
                    anomaly.description()
            );

            context.append("\n");
        }
    }
}