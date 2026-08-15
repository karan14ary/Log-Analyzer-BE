package com.coe.ailoganalyzer.loganalyzerv2.fingerprint;

import com.coe.ailoganalyzer.loganalyzerv2.model.ExceptionInfo;
import com.coe.ailoganalyzer.loganalyzerv2.model.FingerprintInfo;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class FingerprintService {

    private final FingerprintNormalizer normalizer;

    private final ExceptionFingerprintNormalizer
            exceptionNormalizer;

    public FingerprintService(
            FingerprintNormalizer normalizer,
            ExceptionFingerprintNormalizer exceptionNormalizer) {

        this.normalizer = normalizer;
        this.exceptionNormalizer =
                exceptionNormalizer;
    }

    public FingerprintInfo fingerprint(
            String message) {

        String normalized =
                normalizer.normalize(message);

        return new FingerprintInfo(
                sha256(normalized),
                normalized
        );
    }

    public FingerprintInfo fingerprintException(
            ExceptionInfo exceptionInfo) {

        String normalized =
                exceptionNormalizer.normalize(
                        exceptionInfo
                );

        return new FingerprintInfo(
                sha256(normalized),
                normalized
        );
    }

    private String sha256(
            String value) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            for (byte b : hash) {

                result.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 not available",
                    e
            );
        }
    }
}