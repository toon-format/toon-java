package dev.toonformat.jtoon.util;

/**
 * Handles string escaping for TOON format.
 * Escapes special characters that need protection in quoted strings.
 */
public final class StringEscaper {
    private static final int CONTROL_CHAR_MAX = 0x1F;
    private static final int HEX_RADIX = 16;
    private static final int UNICODE_HEX_LENGTH = 4;
    private static final int UNICODE_ESCAPE_TOTAL_LENGTH = 6; // \\uXXXX
    private static final String INVALID_ESCAPE_U = "Invalid escape sequence: \\u";
    private static final String INVALID_UNICODE_LONE_LOW = "Invalid unicode escape: lone low surrogate";
    private static final String INVALID_UNICODE_LONE_HIGH = "Invalid unicode escape: lone high surrogate";

    private StringEscaper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Escapes special characters in a string.
     * Handles backslashes, quotes, and control characters.
     *
     * @param value The string to escape
     * @return The escaped string
     */
    public static String escape(final String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        final int len = value.length();
        final StringBuilder sb = new StringBuilder(len + HEX_RADIX);

        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c <= CONTROL_CHAR_MAX) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * Validates a quoted string for invalid escape sequences and unterminated strings.
     *
     * @param value The string to validate
     * @throws IllegalArgumentException if the string has invalid escape sequences or is unterminated
     */
    public static void validateString(final String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        validateQuotedString(value);
    }

    /**
     * Validates a quoted string for untermination and invalid escape
     * sequences; unquoted values pass through.
     *
     * @param value the string to validate
     */
    private static void validateQuotedString(final String value) {
        if (!value.startsWith("\"") || !value.endsWith("\"")) {
            // Check for unterminated string (starts with quote but doesn't end with quote)
            if (value.startsWith("\"")) {
                throw new IllegalArgumentException("Unterminated string");
            }
            return;
        }
        scanForInvalidEscapes(value.substring(1, value.length() - 1));
    }

    /**
     * Scans an unquoted string content for invalid escape sequences,
     * including a trailing backslash.
     *
     * @param unquoted the unquoted string content
     */
    private static void scanForInvalidEscapes(final String unquoted) {
        boolean escaped = false;
        int i = 0;
        while (i < unquoted.length()) {
            final char c = unquoted.charAt(i);
            if (escaped) {
                // Check if escape sequence is valid
                validateEscapeSequence(c);
                if (c == 'u') {
                    i = validateUnicodeEscape(unquoted, i);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            }
            i++;
        }

        // Check for trailing backslash (invalid escape)
        if (escaped) {
            throw new IllegalArgumentException("Invalid escape sequence: trailing backslash");
        }
    }

    /**
     * Rejects characters that are not valid after a backslash.
     *
     * @param c the character following a backslash
     */
    private static void validateEscapeSequence(final char c) {
        if (!isValidEscapeChar(c)) {
            throw new IllegalArgumentException("Invalid escape sequence: \\" + c);
        }
    }

    /**
     * Validates the {@code \\uXXXX} escape starting at the given index,
     * including a following low-surrogate escape of a surrogate pair.
     *
     * @param unquoted the unquoted string content
     * @param i        the index of the 'u' of the escape
     * @return the index to continue scanning from, after a validated
     *         surrogate pair; unchanged for a single code unit
     */
    private static int validateUnicodeEscape(final String unquoted, final int i) {
        if (i + UNICODE_HEX_LENGTH >= unquoted.length()) {
            throw new IllegalArgumentException(INVALID_ESCAPE_U);
        }
        final String hex = unquoted.substring(i + 1, i + 1 + UNICODE_HEX_LENGTH);
        if (!isHexString(hex)) {
            throw new IllegalArgumentException(INVALID_ESCAPE_U + hex);
        }
        final int codePoint = Integer.parseInt(hex, HEX_RADIX);
        if (Character.isLowSurrogate((char) codePoint)) {
            throw new IllegalArgumentException(INVALID_UNICODE_LONE_LOW);
        }
        if (Character.isHighSurrogate((char) codePoint)) {
            final int nextEscapeStart = i + 1 + UNICODE_HEX_LENGTH;
            if (nextEscapeStart + UNICODE_ESCAPE_TOTAL_LENGTH - 1 >= unquoted.length()
                || unquoted.charAt(nextEscapeStart) != '\\'
                || unquoted.charAt(nextEscapeStart + 1) != 'u') {
                throw new IllegalArgumentException(INVALID_UNICODE_LONE_HIGH);
            }
            final String nextHex = unquoted.substring(nextEscapeStart + 2,
                nextEscapeStart + 2 + UNICODE_HEX_LENGTH);
            if (!isHexString(nextHex)
                || !Character.isLowSurrogate((char) Integer.parseInt(nextHex, HEX_RADIX))) {
                throw new IllegalArgumentException(INVALID_UNICODE_LONE_HIGH);
            }
            // Skip past the full surrogate pair (\\uXXXX\\uXXXX = 12 chars total)
            // to avoid reprocessing the consumed hex digits and the low surrogate
            // escape as individual characters.
            return i + UNICODE_ESCAPE_TOTAL_LENGTH + UNICODE_HEX_LENGTH;
        }
        return i;
    }

    /**
     * Checks if a character is a valid escape sequence.
     */
    private static boolean isValidEscapeChar(final char c) {
        return c == 'n' || c == 'r' || c == 't' || c == '"' || c == '\\' || c == 'u';
    }

    /**
     * Unescapes a string and removes surrounding quotes if present.
     * Reverses the escaping applied by {@link #escape(String)}.
     *
     * @param value The string to unescape (may be quoted)
     * @return The unescaped string with quotes removed
     */
    public static String unescape(final String value) {
        if (value == null || value.length() < 2) {
            return value;
        }

        String unquoted = value;
        if (value.startsWith("\"") && value.endsWith("\"")) {
            unquoted = value.substring(1, value.length() - 1);
        }

        final StringBuilder result = new StringBuilder();
        boolean escaped = false;

        int i = 0;
        while (i < unquoted.length()) {
            final char c = unquoted.charAt(i);
            if (escaped) {
                if (c == 'u') {
                    i = appendUnicodeEscape(result, unquoted, i);
                } else {
                    result.append(unescapeChar(c));
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
            i++;
        }

        return result.toString();
    }

    /**
     * Appends the decoded {@code \\uXXXX} escape starting at the given index,
     * including a following low-surrogate escape of a surrogate pair.
     *
     * @param result   the builder receiving the decoded characters
     * @param unquoted the unquoted string content
     * @param i        the index of the 'u' of the escape
     * @return the index to continue scanning from, after a decoded surrogate
     *         pair; otherwise the index of the last hex digit
     */
    private static int appendUnicodeEscape(final StringBuilder result, final String unquoted, final int i) {
        if (i + UNICODE_HEX_LENGTH >= unquoted.length()) {
            throw new IllegalArgumentException(INVALID_ESCAPE_U);
        }
        final String hex = unquoted.substring(i + 1, i + 1 + UNICODE_HEX_LENGTH);
        if (!isHexString(hex)) {
            throw new IllegalArgumentException(INVALID_ESCAPE_U + hex);
        }
        final char codeUnit = (char) Integer.parseInt(hex, HEX_RADIX);
        if (Character.isLowSurrogate(codeUnit)) {
            throw new IllegalArgumentException(INVALID_UNICODE_LONE_LOW);
        }
        if (Character.isHighSurrogate(codeUnit)) {
            if (i + (2 * UNICODE_ESCAPE_TOTAL_LENGTH) - 2 >= unquoted.length()
                || unquoted.charAt(i + 1 + UNICODE_HEX_LENGTH) != '\\'
                || unquoted.charAt(i + 2 + UNICODE_HEX_LENGTH) != 'u') {
                throw new IllegalArgumentException(INVALID_UNICODE_LONE_HIGH);
            }
            final String lowHex = unquoted.substring(i + 3 + UNICODE_HEX_LENGTH,
                i + 3 + (2 * UNICODE_HEX_LENGTH));
            if (!isHexString(lowHex)) {
                throw new IllegalArgumentException(INVALID_ESCAPE_U + lowHex);
            }
            final char lowCodeUnit = (char) Integer.parseInt(lowHex, HEX_RADIX);
            if (!Character.isLowSurrogate(lowCodeUnit)) {
                throw new IllegalArgumentException(INVALID_UNICODE_LONE_HIGH);
            }
            result.append(codeUnit);
            result.append(lowCodeUnit);
            return i + (2 * UNICODE_ESCAPE_TOTAL_LENGTH) - 2;
        }
        result.append(codeUnit);
        return i + UNICODE_HEX_LENGTH;
    }

    private static boolean isHexString(final String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), HEX_RADIX) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts an escaped character to its unescaped form.
     *
     * @param c The character following a backslash
     * @return The unescaped character
     * @throws IllegalArgumentException if the escape sequence is invalid
     */
    private static char unescapeChar(final char c) {
        return switch (c) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case '"' -> '"';
            case '\\' -> '\\';
            default -> throw new IllegalArgumentException("Invalid escape sequence: \\" + c);
        };
    }
}
