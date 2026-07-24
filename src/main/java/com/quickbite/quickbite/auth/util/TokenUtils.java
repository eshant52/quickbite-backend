package com.quickbite.quickbite.auth.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Refresh tokens are high-entropy random strings, not user-chosen secrets,
 * so SHA-256 is sufficient here — unlike passwords, brute-forcing isn't the
 * threat model; token theft in transit/storage is. We never store the raw
 * token, only its hash, mirroring how passwords are handled but with a
 * cheaper hash appropriate for this threat model.
 */
public class TokenUtils {
    private static final SecureRandom secureRandom = new SecureRandom();

    private TokenUtils() {}

    public static String generateOpaqueToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while hashing the input", e);
        }
    }
}
