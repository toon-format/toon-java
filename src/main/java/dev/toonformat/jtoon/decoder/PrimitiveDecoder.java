package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.util.StringEscaper;

/**
 * Handles parsing of primitive TOON values with type inference.
 *
 * <p>
 * Converts TOON scalar representations to appropriate Java types:
 * </p>
 * <ul>
 * <li>{@code "null"} → {@code null}</li>
 * <li>{@code "true"} / {@code "false"} → {@code Boolean}</li>
 * <li>Numeric strings → {@code Long} or {@code Double}</li>
 * <li>Quoted strings → {@code String} (with unescaping)</li>
 * <li>Bare strings → {@code String}</li>
 * </ul>
 *
 * <h2>Examples:</h2>
 *
 * <pre>{@code
 * parse("null")      → null
 * parse("true")      → true
 * parse("42")        → 42L
 * parse("3.14")      → 3.14
 * parse("\"hello\"") → "hello"
 * parse("hello")     → "hello"
 * parse("")          → "" (empty string)
 * }</pre>
 */
final class PrimitiveDecoder {

    private PrimitiveDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parses a TOON primitive value and infers its type.
     *
     * @param value The string representation of the value
     * @return The parsed value as {@code Boolean}, {@code Long}, {@code Double},
     * {@code String}, or {@code null}
     */
    static Object parse(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        // Check for null literal
        switch (value) {
            case "null" -> {
                return null;
            }
            case "true" -> {
                return true;
            }
            case "false" -> {
                return false;
            }
            default -> {
                // Do nothing, continue to next check
            }
        }

        // Check for quoted strings
        if (value.startsWith("\"")) {
            // Validate string before unescaping
            StringEscaper.validateString(value);
            return StringEscaper.unescape(value);
        }

        // Check for leading zeros (treat as string, except for "0", "-0", "0.0", etc.)
        String trimmed = value.trim();
        if (trimmed.length() > 1
            && trimmed.matches("^-?0+[0-7].*") //octal number
            && !trimmed.matches("^-?0+(\\.0+)?([eE][+-]?\\d+)?$")) {
            return value;
        }

        // Try parsing as number
        try {
            // Check if it contains exponent notation or decimal point
            if (value.contains(".") || value.contains("e") || value.contains("E")) {
                double parsed = Double.parseDouble(value);
                // Handle negative zero - Java doesn't distinguish, but spec says it should be 0
                if (parsed == 0.0) {
                    return 0L;
                }
                // Check if the result is a whole number - if so, return as Long
                if (parsed == Math.floor(parsed)
                    && !Double.isInfinite(parsed)
                    && parsed >= Long.MIN_VALUE
                    && parsed <= Long.MAX_VALUE) {
                    return (long) parsed;
                }

                return parsed;
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
