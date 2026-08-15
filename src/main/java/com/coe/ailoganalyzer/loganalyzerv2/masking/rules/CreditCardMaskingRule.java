package com.coe.ailoganalyzer.loganalyzerv2.masking.rules;


import com.coe.ailoganalyzer.loganalyzerv2.masking.MaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CreditCardMaskingRule implements MaskingRule {

    private static final Pattern PATTERN =
            Pattern.compile(
                    "(?<!\\d)"
                            + "(?:\\d[ -]*?){13,19}"
                            + "(?!\\d)"
            );

    @Override
    public MaskingType type() {
        return MaskingType.CREDIT_CARD;
    }

    @Override
    public Pattern pattern() {
        return PATTERN;
    }

    @Override
    public String replacement(String matchedValue) {
        return  "[CARD_REDACTED]";
    }

}