package com.coe.ailoganalyzer.loganalyzerv2.masking.rules;


import com.coe.ailoganalyzer.loganalyzerv2.masking.MaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ApiKeyMaskingRule implements MaskingRule {

    private static final Pattern PATTERN =
            Pattern.compile(
                    "(?i)(api[_-]?key|client[_-]?secret)"
                            + "\\s*[=:]\\s*"
                            + "(\"[^\"]*\"|'[^']*'|[^\\s,;]+)"
            );

    @Override
    public MaskingType type() {
        return MaskingType.API_KEY;
    }

    @Override
    public Pattern pattern() {
        return PATTERN;
    }

    @Override
    public String replacement(
            String matchedValue) {

        int separator =
                findSeparator(matchedValue);

        if (separator == -1) {
            return "[API_KEY_REDACTED]";
        }

        String key =
                matchedValue
                        .substring(0, separator)
                        .trim();

        return key + "=[API_KEY_REDACTED]";
    }

    private int findSeparator(
            String value) {

        int equals =
                value.indexOf('=');

        int colon =
                value.indexOf(':');

        if (equals == -1) {
            return colon;
        }

        if (colon == -1) {
            return equals;
        }

        return Math.min(
                equals,
                colon
        );
    }

}