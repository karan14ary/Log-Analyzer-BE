package com.coe.ailoganalyzer.loganalyzerv2.report;


import com.coe.ailoganalyzer.loganalyzerv2.model.Anomaly;
import com.coe.ailoganalyzer.loganalyzerv2.model.IncidentReport;
import org.springframework.stereotype.Component;

@Component
public class IncidentMarkdownRenderer {

    public String render(
            IncidentReport report) {

        StringBuilder markdown =
                new StringBuilder();

        markdown.append("# ")
                .append(report.title())
                .append("\n\n");

        markdown.append("## Incident\n\n");

        markdown.append("| Field | Value |\n");
        markdown.append("|---|---|\n");

        markdown.append("| Incident ID | ")
                .append(report.incidentId())
                .append(" |\n");

        markdown.append("| Severity | ")
                .append(report.severity())
                .append(" |\n");

        markdown.append("| Start | ")
                .append(report.startTime())
                .append(" |\n");

        markdown.append("| End | ")
                .append(report.endTime())
                .append(" |\n");

        markdown.append("| Duration | ")
                .append(report.duration())
                .append(" |\n");

        markdown.append("| Total Events | ")
                .append(report.totalEvents())
                .append(" |\n");

        markdown.append("| Errors | ")
                .append(report.totalErrors())
                .append(" |\n");

        markdown.append("| Affected Traces | ")
                .append(report.affectedTraces())
                .append(" |\n\n");

        markdown.append("## Summary\n\n");

        markdown.append(report.summary())
                .append("\n\n");

        markdown.append("## Root Cause\n\n");

        if (report.rca() != null
                && report.rca().successful()
                && report.rca().result() != null) {

            var rca =
                    report.rca().result();

            markdown.append("**")
                    .append(rca.rootCause())
                    .append("**\n\n");

            markdown.append(
                    "Confidence: "
            ).append(
                    rca.confidence()
            ).append("\n\n");

            markdown.append("### Evidence\n\n");

            for (Object evidence :
                    rca.evidence()) {

                markdown.append("- ")
                        .append(String.valueOf(evidence))
                        .append("\n");
            }

            markdown.append("\n");
        }

        markdown.append("## Anomalies\n\n");

        for (Anomaly anomaly :
                report.anomalies()) {

            markdown.append("- **")
                    .append(anomaly.type())
                    .append("** — ")
                    .append(anomaly.severity())
                    .append(" — ")
                    .append(anomaly.description())
                    .append("\n");
        }

        markdown.append("\n");

        markdown.append("## Recommendations\n\n");

        for (String recommendation :
                report.recommendations()) {

            markdown.append("- ")
                    .append(recommendation)
                    .append("\n");
        }

        return markdown.toString();
    }
}