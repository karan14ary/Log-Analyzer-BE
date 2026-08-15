package com.coe.ailoganalyzer.loganalyzerv2.masking;


import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.MaskingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogMaskingService {

    private final MaskingEngine maskingEngine;

    public MaskingResult mask(LogEvent event) {

        return maskingEngine.mask(
                event.rawLog()
        );
    }

    public MaskingResult mask(String text) {

        return maskingEngine.mask(text);
    }
}