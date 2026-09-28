package com.coe.ailoganalyzer.loganalyzerv2.masking;

import com.coe.ailoganalyzer.loganalyzerv2.masking.rules.ApiKeyMaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.masking.rules.CreditCardMaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.masking.rules.EmailMaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.masking.rules.JwtMaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.masking.rules.PasswordMaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.masking.rules.PhoneMaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.masking.rules.TokenMaskingRule;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogEvent;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogFormat;
import com.coe.ailoganalyzer.loganalyzerv2.model.LogLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskingBehaviorTest {

    private final MaskingEngine engine = new MaskingEngine(List.of(
            new EmailMaskingRule(),
            new PasswordMaskingRule(),
            new TokenMaskingRule(),
            new PhoneMaskingRule(),
            new JwtMaskingRule(),
            new CreditCardMaskingRule(),
            new ApiKeyMaskingRule()
    ));

    @Test
    void masksSensitiveValuesAndReportsFindings() {
        String token = "eyJhbGci.eyJzdWI.eyJzaWQ";
        String input = "email=user@example.com password='secret' access_token=abc "
                + "authorization=Bearer xyz phone=+1 212-555-0123 jwt=" + token
            + " api_key=key123";

        var result = engine.mask(input);

        assertTrue(result.maskedText().contains("[EMAIL_REDACTED]"));
        assertTrue(result.maskedText().contains("password=[SECRET_REDACTED]"));
        assertTrue(result.maskedText().contains("[TOKEN_REDACTED]"));
        assertTrue(result.maskedText().contains("[PHONE_REDACTED]"));
        assertTrue(result.maskedText().contains("[JWT_REDACTED]"));
        assertTrue(result.maskedText().contains("api_key=[API_KEY_REDACTED]"));
        assertEquals(7, result.findings().size());
    }

    @Test
    void masksCreditCardsWhenTheyAreNotOverlappedByPhoneRules() {
        var cardEngine = new MaskingEngine(List.of(new CreditCardMaskingRule()));

        assertEquals("card=[CARD_REDACTED]", cardEngine.mask("card=4111111111111111").maskedText());
    }

    @Test
    void blankAndNullInputsAreReturnedWithoutFindings() {
        assertEquals(null, engine.mask(null).maskedText());
        assertEquals(" \t", engine.mask(" \t").maskedText());
        assertTrue(engine.mask(null).findings().isEmpty());
        assertTrue(engine.mask("").findings().isEmpty());
    }

    @Test
    void repeatedFindingsHavePerRuleOccurrenceNumbers() {
        var result = engine.mask("a@b.com and c@d.com");

        assertEquals("[EMAIL_REDACTED] and [EMAIL_REDACTED]", result.maskedText());
        assertEquals(1, result.findings().get(0).occurrence());
        assertEquals(2, result.findings().get(1).occurrence());
    }

    @Test
    void serviceMasksRawLogFromEventAndTextDirectly() {
        var service = new LogMaskingService(engine);
        var event = new LogEvent(null, LogLevel.INFO, LogFormat.GENERIC, null, null,
                null, "message", null, null, null, "email=user@example.com", Map.of());

        assertTrue(service.mask(event).maskedText().contains("[EMAIL_REDACTED]"));
        assertEquals("no secrets", service.mask("no secrets").maskedText());
    }
}
