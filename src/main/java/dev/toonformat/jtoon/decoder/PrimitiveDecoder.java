package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.util.StringEscaper;
import java.util.regex.Pattern;
import static dev.toonformat.jtoon.util.Constants.DOT;
import static dev.toonformat.jtoon.util.Constants.FALSE_LITERAL;
import static dev.toonformat.jtoon.util.Constants.NULL_LITERAL;
import static dev.toonformat.jtoon.util.Constants.TRUE_LITERAL;

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
public final class PrimitiveDecoder {

    // Normative number grammar of TOON spec §4: ^-?[0-9]+(?:\.[0-9]+)?(?:e[+-]?[0-9]+)?$
    // (case-insensitive). No leading '+': that is the wider encoder-side
    // numeric-like test of §7.2. Tokens failing the gate (.5, 1., +1, NaN, 0x10)
    // decode as strings without delegating to a host-language number parser.
    private static final Pattern NUMBER_GRAMMAR = Pattern.compile("^-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?$");

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
    @SuppressWarnings("NullAway")
    static Object parse(final String value) {
        return parse(value, Integer.MAX_VALUE);
    }

    @SuppressWarnings("NullAway")
    static Object parse(final String value, final DecodeContext context) {
        return parse(value, context.options.maxStringLength());
    }

    @SuppressWarnings("NullAway")
    static Object parse(final String value, final int maxStringLength) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() > maxStringLength) {
            throw new IllegalArgumentException(
                "String length " + value.length() + " exceeds maximum allowed " + maxStringLength);
        }

        // Check for null literal
        switch (value) {
            case NULL_LITERAL -> {
                return null;
            }
            case TRUE_LITERAL -> {
                return true;
            }
            case FALSE_LITERAL -> {
                return false;
            }
            default -> {
                // Do nothing, continue to next check
            }
        }

        // Check for quoted strings
        if (value.startsWith("\"")) {
            // Spec §7.4: a quoted token is terminated by its closing quote;
            // only whitespace may follow within the same token. The boundary
            // rule applies in strict and non-strict mode alike.
            validateQuotedTokenBoundary(value);
            // Validate string before unescaping
            StringEscaper.validateString(value);
            return StringEscaper.unescape(value);
        }

        return parseNumericToken(value);
    }

    /**
     * Parses an unquoted token as a number. Tokens failing the normative
     * number grammar gate or carrying forbidden leading zeros decode as
     * strings, without delegating to a host-language number parser (§4).
     *
     * @param value the token to parse
     * @return the parsed number, or the token as string
     */
    private static Object parseNumericToken(final String value) {
        final String trimmed = value.trim();

        // Normative number grammar gate (§4): tokens that do not match decode as
        // strings, without delegating to a host-language number parser (§4).
        if (!NUMBER_GRAMMAR.matcher(trimmed).matches()) {
            return value;
        }

        if (hasForbiddenLeadingZeros(trimmed)) {
            return value; // treat as string
        }

        // Try parsing as number
        try {
            // Check if it contains exponent notation or decimal point
            if (isFloatingPointToken(value)) {
                return normalizeFloatingPoint(Double.parseDouble(value));
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    /**
     * Per spec §4: tokens like "05", "0001", "-05", "-0001" must be treated
     * as strings. But "0.5", "0e1", "-0.5", "-0e1" are valid numbers.
     *
     * @param trimmed the trimmed token to check
     * @return true when the token carries forbidden leading zeros
     */
    private static boolean hasForbiddenLeadingZeros(final String trimmed) {
        if (trimmed.length() <= 1) {
            return false;
        }
        // Match forbidden leading zeros: starts with optional '-', then one or more zeros,
        // then another digit (0-9) — meaning it's a multi-digit number with leading zeros.
        // Exclude cases where the zero is part of a fractional/exponent form like "0.5", "0e1".
        final boolean hasLeadingZeros = trimmed.matches("^-?0+\\d.*");
        // But we must NOT match "0.5" style numbers (single zero integer part)
        final boolean isLikelyFractionalOrExponent = trimmed.matches("^-?0[.eE].*");
        return hasLeadingZeros && !isLikelyFractionalOrExponent;
    }

    /**
     * Returns whether the token contains exponent notation or a decimal point.
     *
     * @param value the token to check
     * @return true for floating point tokens
     */
    private static boolean isFloatingPointToken(final String value) {
        return value.contains("e") || value.contains("E") || value.contains(DOT);
    }

    /**
     * Normalizes a parsed floating point value: negative zero collapses to
     * zero, and whole numbers within long range are returned as Long.
     *
     * @param parsed the parsed double value
     * @return the normalized number
     */
    private static Object normalizeFloatingPoint(final double parsed) {
        // Handle negative zero - Java doesn't distinguish, but spec says it should be 0
        if (parsed == 0.0) {
            return 0L;
        }
        // Check if the result is a whole number - if so, return as Long
        if (!Double.isInfinite(parsed)
            && parsed >= Long.MIN_VALUE
            && parsed <= Long.MAX_VALUE
            && parsed == Math.floor(parsed)) {
            return (long) parsed;
        }

        return parsed;
    }

    /**
     * Spec §7.4: after the closing quote of a quoted token only whitespace may
     * follow. An unterminated token is left to {@link StringEscaper#validateString}.
     *
     * @param value the token to validate
     */
    private static void validateQuotedTokenBoundary(final String value) {
        boolean escaped = false;
        for (int i = 1; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                for (int j = i + 1; j < value.length(); j++) {
                    if (!Character.isWhitespace(value.charAt(j))) {
                        throw new FatalDecodeException(
                            "Characters after closing quote in token: " + value);
                    }
                }
                return;
            }
        }
    }
}
