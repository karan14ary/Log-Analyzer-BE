package com.coe.ailoganalyzer.loganalyzerv2.parser;



import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LogEntrySplitter {

    public List<String> split(String content) {

        String[] lines =
                content.split("\\R");

        List<String> entries =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        for (String line : lines) {

            if (isNewEntry(line)
                    && !current.isEmpty()) {

                entries.add(
                        current.toString().trim()
                );

                current.setLength(0);
            }

            if (!line.isBlank()) {

                current.append(line)
                        .append(System.lineSeparator());
            }
        }

        if (!current.isEmpty()) {

            entries.add(
                    current.toString().trim()
            );
        }

        return entries;
    }

    private boolean isNewEntry(String line) {

        String value =
                line.trim();

        return value.matches(
                "^\\d{4}-\\d{2}-\\d{2}.*"
        )
                || value.startsWith("{");
    }
}