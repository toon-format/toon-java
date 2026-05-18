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

        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            switch (c) {
                case ':':
                case '"':
                case '\\':
                case '[':
                case ']':
                case '{':
                case '}':
                case '\n':
                case '\r':
                case '\t':
                    return false;
                default:
                    if (c <= CONTROL_CHAR_MAX) {
                        return false;
                    }
                    if (delimiter.length() == 1 && c == delimiter.charAt(0)) {
                        return false;
                    }
            }
        }

        return !value.startsWith(LIST_ITEM_MARKER);
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

        if (!Character.isJavaIdentifierStart(first) && first != '_') {
            return false;
        }

        for (int i = 1; i < len; i++) {
            final char c = key.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.') {
                return false;
            }
        }

        return true;
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

        final int len = value.length();
        int i = 0;

        if (value.charAt(0) == '-') {
            if (len < 2) {
                return false;
            }
            i = 1;
        }

        boolean hasDigit = false;
        boolean hasDot = false;
        boolean hasExponent = false;

        while (i < len) {
            final char c = value.charAt(i);

            if (c >= '0' && c <= '9') {
                hasDigit = true;
            } else if (c == '.') {
                if (hasDot || hasExponent || !hasDigit) {
                    return false;
                }
                hasDot = true;
                hasDigit = false;
            } else if (c == 'e' || c == 'E') {
                if (!hasDigit || hasExponent) {
                    return false;
                }
                hasExponent = true;
                hasDigit = false;
                if (i + 1 < len) {
                    final char next = value.charAt(i + 1);
                    if (next == '+' || next == '-') {
                        i++;
                    }
                }
            } else {
                return false;
            }
            i++;
        }

        return hasDigit;
    }

    static boolean containsQuotesOrBackslash(final String value) {
        return value.indexOf(DOUBLE_QUOTE) >= 0
            || value.indexOf(BACKSLASH) >= 0;
    }
}
