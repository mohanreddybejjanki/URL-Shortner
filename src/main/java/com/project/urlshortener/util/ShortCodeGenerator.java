package com.project.urlshortener.util;

import org.springframework.stereotype.Component;

/**
 * Utility class to generate unique short codes using Base62 encoding.
 *
 * ── What is Base62? ──────────────────────────────────────────────────────────
 * Base62 uses 62 characters: a-z (26) + A-Z (26) + 0-9 (10) = 62 total.
 * This is URL-safe (no special chars like + / = from Base64).
 *
 * ── How does it work? ────────────────────────────────────────────────────────
 * We generate a random long integer and repeatedly:
 *   1. Take remainder when divided by 62  → index into BASE62_CHARS
 *   2. Divide the number by 62
 *   3. Prepend the character to the result
 *   4. Repeat until the code is TARGET_LENGTH characters long
 *
 * Example:
 *   Input number:  12345
 *   12345 % 62 = 57 → '5'
 *   12345 / 62 = 199
 *   199 % 62 = 13   → 'N'
 *   199 / 62 = 3
 *   3 % 62 = 3      → 'd'
 *   Result: "dN5"  (padded to TARGET_LENGTH)
 *
 * ── Collision probability ────────────────────────────────────────────────────
 * With 6-char codes: 62^6 = 56 billion combinations.
 * With 8-char codes: 62^8 = 218 trillion combinations.
 * Collision at 6 chars becomes likely only after ~7.5 million URLs (birthday paradox).
 * The repository layer handles the rare collision case via existsByShortCode check.
 */
@Component
public class ShortCodeGenerator {

    private static final String BASE62_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /** Length of the generated short code. 6 chars = 56 billion combinations. */
    private static final int TARGET_LENGTH = 6;

    /**
     * Generates a random Base62-encoded short code.
     *
     * Uses Math.random() * Long.MAX_VALUE to get a random seed.
     * The service layer calls this in a retry loop if a collision occurs.
     *
     * @return a 6-character alphanumeric string, e.g. "aB3xY2"
     */
    public String generate() {
        long randomNumber = (long) (Math.random() * Long.MAX_VALUE);
        return encodeBase62(randomNumber);
    }

    /**
     * Core Base62 encoding algorithm.
     *
     * @param number any positive long integer
     * @return Base62 string padded/trimmed to TARGET_LENGTH
     */
    private String encodeBase62(long number) {
        StringBuilder sb = new StringBuilder();

        // Build code character by character
        while (sb.length() < TARGET_LENGTH) {
            int remainder = (int) (number % 62);
            sb.append(BASE62_CHARS.charAt(remainder));
            number = number / 62;

            // If number is exhausted before TARGET_LENGTH, use random fill
            if (number == 0) {
                number = (long) (Math.random() * Long.MAX_VALUE);
            }
        }

        // Reverse because we built it right-to-left
        return sb.reverse().toString().substring(0, TARGET_LENGTH);
    }
}