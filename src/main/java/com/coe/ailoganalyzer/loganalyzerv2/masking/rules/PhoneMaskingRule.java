package com.coe.ailoganalyzer.loganalyzerv2.masking.rules;


import com.coe.ailoganalyzer.loganalyzerv2.masking.MaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PhoneMaskingRule implements MaskingRule {

    private static final Pattern PATTERN =
            Pattern.compile(
                    "(?<!\\d)(?:\\+?\\d[\\d\\s-]{8,14}\\d)(?!\\d)"
            );

    @Override
    public MaskingType type() {
        return MaskingType.PHONE;
    }

    @Override
    public Pattern pattern() {
        return PATTERN;
    }

    @Override
    public String replacement(String matchedValue) {
        return "[PHONE_REDACTED]";
    }

}