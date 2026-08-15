package com.coe.ailoganalyzer.loganalyzerv2.masking.rules;

import com.coe.ailoganalyzer.loganalyzerv2.masking.MaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class JwtMaskingRule implements MaskingRule {

    private static final Pattern PATTERN =
            Pattern.compile(
                    "\\beyJ[A-Za-z0-9_-]+\\."
                            + "[A-Za-z0-9_-]+\\."
                            + "[A-Za-z0-9_-]+\\b"
            );

    @Override
    public MaskingType type() {
        return MaskingType.JWT;
    }

    @Override
    public Pattern pattern() {
        return PATTERN;
    }

    @Override
    public String replacement(String matchedValue) {
        return "[JWT_REDACTED]";
    }

}