package com.coe.ailoganalyzer.loganalyzerv2.fingerprint;



import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class FingerprintNormalizer {

    private static final Pattern UUID_PATTERN =
            Pattern.compile(
                    "\\b[0-9a-fA-F]{8}-"
                            + "[0-9a-fA-F]{4}-"
                            + "[0-9a-fA-F]{4}-"
                            + "[0-9a-fA-F]{4}-"
                            + "[0-9a-fA-F]{12}\\b"
            );

    private static final Pattern IP_PATTERN =
            Pattern.compile(
                    "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"
            );

    private static final Pattern NUMBER_PATTERN =
            Pattern.compile(
                    "\\b\\d+\\b"
            );

    private static final Pattern HEX_PATTERN =
            Pattern.compile(
                    "\\b0x[0-9a-fA-F]+\\b"
            );
    private static final Pattern REQUEST_ID_PATTERN =
            Pattern.compile(
                    "(?i)(request[_-]?id|correlation[_-]?id|trace[_-]?id)"
                            + "\\s*[=:]\\s*"
                            + "[\\w-]+"
            );

    public String normalize(
            String message) {

        if (message == null
                || message.isBlank()) {

            return message;
        }

        String normalized =
                message;

        normalized =
                UUID_PATTERN.matcher(normalized)
                        .replaceAll("<UUID>");

        normalized =
                IP_PATTERN.matcher(normalized)
                        .replaceAll("<IP>");

        normalized =
                HEX_PATTERN.matcher(normalized)
                        .replaceAll("<HEX>");

        normalized =
                NUMBER_PATTERN.matcher(normalized)
                        .replaceAll("<NUMBER>");
        normalized =
                REQUEST_ID_PATTERN.matcher(normalized)
                        .replaceAll("$1=<ID>");

        return normalized;
    }
}