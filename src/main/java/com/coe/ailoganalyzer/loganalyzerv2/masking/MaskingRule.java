package com.coe.ailoganalyzer.loganalyzerv2.masking;

import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingType;

import java.util.regex.Pattern;

public interface MaskingRule {

    MaskingType type();

    Pattern pattern();

    String replacement(String matchedValue);

}