package com.coe.ailoganalyzer.loganalyzerv2.masking.rules;

import com.coe.ailoganalyzer.loganalyzerv2.masking.MaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class TokenMaskingRule implements MaskingRule {

    private static final Pattern PATTERN =
            Pattern.compile(
                    "(?i)"
                            + "(access[_-]?token)"
                            + "\\s*[=:]\\s*"
                            + "(\"[^\"]*\"|'[^']*'|[^\\s,;]+)"
                            + "|"
                            + "(authorization)"
                            + "\\s*[=:]\\s*"
                            + "(Bearer\\s+[^\\s,;]+)"
            );

    @Override
    public MaskingType type() {
        return MaskingType.ACCESS_TOKEN;
    }

    @Override
    public Pattern pattern() {
        return PATTERN;
    }

    @Override
    public String replacement(String matchedValue) {
        return "[TOKEN_REDACTED]";
    }
}