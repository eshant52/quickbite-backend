package com.quickbite.quickbite.common.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Generates human-friendly, collision-proof, time-ordered transaction IDs.
 * Format:  PAY-YYMMDD-XXXXXXXX
 * Example: PAY-260913-7N4K9P2X
 * <p>
 * Characteristics:
 * - 19 characters total (compact, easy to read over phone/SMS)
 * - Date prefix: gives instant chronological context for debugging & customer support
 * - Crockford Base32: avoids 0/O and 1/I/L confusion
 * - 8-character random suffix: 32^8 = ~1.1 trillion combinations per day
 */
@Component
public class TransactionIdGenerator {

    // Crockford's Base32 alphabet (no I, L, O, U to prevent visual confusion & accidental profanity)
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int RANDOM_PART_LENGTH = 8;

    private static final DateTimeFormatter DATE_PREFIX_FORMATTER = DateTimeFormatter
            .ofPattern("yyMMdd")
            .withZone(ZoneOffset.UTC);

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a unique transaction ID with the default "PAY" prefix.
     * Example: PAY-260913-7N4K9P2X
     */
    public String generate() {
        return generate("PAY");
    }

    /**
     * Generates a unique transaction ID with a custom prefix (e.g., "COD", "REF").
     * Example: COD-260913-K9P2X7N4
     */
    public String generate(String prefix) {
        String datePart = DATE_PREFIX_FORMATTER.format(Instant.now());

        char[] randomPart = new char[RANDOM_PART_LENGTH];
        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            randomPart[i] = ALPHABET[secureRandom.nextInt(ALPHABET.length)];
        }

        return prefix + "-" + datePart + "-" + new String(randomPart);
    }
}
