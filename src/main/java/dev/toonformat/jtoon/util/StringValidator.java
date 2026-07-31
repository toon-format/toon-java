package dev.toonformat.jtoon.util;

import static dev.toonformat.jtoon.util.Constants.BACKSLASH;
import static dev.toonformat.jtoon.util.Constants.DOUBLE_QUOTE;
import static dev.toonformat.jtoon.util.Constants.FALSE_LITERAL;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_MARKER;
import static dev.toonformat.jtoon.util.Constants.NULL_LITERAL;
import static dev.toonformat.jtoon.util.Constants.TRUE_LITERAL;

/**
 * Validates strings for safe unquoted usage in TOON format.
 * Uses char-by-char validation for performance instead of regex.
 */
public final class StringValidator {
    private static final int CONTROL_CHAR_MAX = 0x1F;

    private StringValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Checks if a string can be safely written without quotes.
     * Uses char-by-char validation for performance.
     *
     * @param value     the string value to check
     * @param delimiter the delimiter being used (for validation)
     * @return true if the string can be safely written without quotes, false otherwise
     */
    public static boolean isSafeUnquoted(final String value, final String delimiter) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        final int len = value.length();

        if (value.charAt(0) == ' ' || value.charAt(len - 1) == ' ') {
            return false;
        }

        if (isKeyword(value)) {
            return false;
        }

        if (isNumericLike(value)) {
            return false;
        }

        // Spec §7.2: tokens starting with '#' must be quoted (comment marker).
        if (value.charAt(0) == '#') {
            return false;
        }

        if (!hasSafeCharacters(value, delimiter)) {
            return false;
        }

        return !value.startsWith(LIST_ITEM_MARKER);
    }

    /**
     * Rejects any character that would require quoting: structural
     * characters, control characters and the active delimiter.
     *
     * @param value     the string value to check
     * @param delimiter the delimiter being used (for validation)
     * @return true when every character is safe unquoted
     */
    private static boolean hasSafeCharacters(final String value, final String delimiter) {
        final int len = value.length();
        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            switch (c) {
                case ':', '"', '\\', '[', ']', '{', '}', '\n', '\r', '\t' -> {
                    return false;
                }
                default -> {
                    if (c <= CONTROL_CHAR_MAX) {
                        return false;
                    }
                    if (delimiter.length() == 1 && c == delimiter.charAt(0)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Checks if a key can be used without quotes.
     *
     * @param key the key to validate
     * @return true if the key can be used without quotes, false otherwise
     */
    public static boolean isValidUnquotedKey(final String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        final int len = key.length();
        final char first = key.charAt(0);

        // Spec §7.3: unquoted keys must match ^[A-Za-z_][A-Za-z0-9_.]*$ (ASCII only).
        if (!isAsciiLetter(first) && first != '_') {
            return false;
        }

        for (int i = 1; i < len; i++) {
            final char c = key.charAt(i);
            if (!isAsciiLetterOrDigit(c) && c != '_' && c != '.') {
                return false;
            }
        }

        return true;
    }

    private static boolean isAsciiLetter(final char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isAsciiLetterOrDigit(final char c) {
        return isAsciiLetter(c) || (c >= '0' && c <= '9');
    }

    private static boolean isKeyword(final String value) {
        return TRUE_LITERAL.equals(value)
            || FALSE_LITERAL.equals(value)
            || NULL_LITERAL.equals(value);
    }

    private static boolean isNumericLike(final String value) {
        if (value.isEmpty()) {
            return false;
        }
        final int start = skipSign(value);
        if (start < 0) {
            return false;
        }
        return scanNumericBody(value, start);
    }

    /**
     * Skips an optional leading sign. A lone sign is not numeric-like.
     *
     * @param value the value to inspect
     * @return the index after the sign, 0 without a sign, or -1 for a lone sign
     */
    private static int skipSign(final String value) {
        final char first = value.charAt(0);
        if (first != '-' && first != '+') {
            return 0;
        }
        if (value.length() < 2) {
            return -1;
        }
        return 1;
    }

    /**
     * Scans the numeric body after the optional sign. Mirrors the grammar
     * [0-9]+ ('.' [0-9]+)? ([eE] [+-]? [0-9]+)? from Spec §7.2.
     *
     * @param value the value to scan
     * @param start the index where the body starts
     * @return true when the body is numeric-like
     */
    private static boolean scanNumericBody(final String value, final int start) {
        int i = consumeDigits(value, start);
        if (i == start) {
            return false;
        }
        if (i < value.length() && value.charAt(i) == '.') {
            final int afterDot = consumeDigits(value, i + 1);
            if (afterDot == i + 1) {
                return false;
            }
            i = afterDot;
        }
        if (i < value.length() && isExponentMarker(value.charAt(i))) {
            final int afterExponent = consumeExponentPart(value, i + 1);
            if (afterExponent == i + 1) {
                return false;
            }
            i = afterExponent;
        }
        return i == value.length();
    }

    /**
     * Consumes a run of ASCII digits.
     *
     * @param value the value to scan
     * @param from  the index to start at
     * @return the index after the last consumed digit
     */
    private static int consumeDigits(final String value, final int from) {
        int i = from;
        while (i < value.length() && isDigit(value.charAt(i))) {
            i++;
        }
        return i;
    }

    private static boolean isDigit(final char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isExponentMarker(final char c) {
        return c == 'e' || c == 'E';
    }

    /**
     * Consumes the exponent part after the marker: an optional sign
     * followed by at least one digit.
     *
     * @param value the value to scan
     * @param from  the index after the exponent marker
     * @return the index after the last consumed digit
     */
    private static int consumeExponentPart(final String value, final int from) {
        return consumeDigits(value, skipOptionalSign(value, from));
    }

    /**
     * Skips an optional exponent sign.
     *
     * @param value the value to scan
     * @param from  the index to start at
     * @return the index after the sign, or the unchanged index
     */
    private static int skipOptionalSign(final String value, final int from) {
        if (from < value.length()) {
            final char c = value.charAt(from);
            if (c == '+' || c == '-') {
                return from + 1;
            }
        }
        return from;
    }

    static boolean containsQuotesOrBackslash(final String value) {
        return value.indexOf(DOUBLE_QUOTE) >= 0
            || value.indexOf(BACKSLASH) >= 0;
    }
}
