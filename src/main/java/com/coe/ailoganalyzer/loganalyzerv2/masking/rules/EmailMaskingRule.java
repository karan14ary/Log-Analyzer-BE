package com.coe.ailoganalyzer.loganalyzerv2.masking.rules;

import com.coe.ailoganalyzer.loganalyzerv2.masking.MaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class EmailMaskingRule implements MaskingRule {

    private static final Pattern PATTERN =
            Pattern.compile(
                    "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
            );

    @Override
    public MaskingType type() {
        return MaskingType.EMAIL;
    }

    @Override
    public Pattern pattern() {
        return PATTERN;
    }

    @Override
    public String replacement(String matchedValue) {
        return "[EMAIL_REDACTED]";
    }

}