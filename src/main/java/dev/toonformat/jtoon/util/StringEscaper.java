package dev.toonformat.jtoon.util;

/**
 * Handles string escaping for TOON format.
 * Escapes special characters that need protection in quoted strings.
 */
public final class StringEscaper {

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
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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

        // Check for unterminated string (starts with quote but doesn't end with quote)
        if (value.startsWith("\"") && !value.endsWith("\"")) {
            throw new IllegalArgumentException("Unterminated string");
        }

        // Check for invalid escape sequences in quoted strings
        if (value.startsWith("\"") && value.endsWith("\"")) {
            final String unquoted = value.substring(1, value.length() - 1);
            boolean escaped = false;

            for (char c : unquoted.toCharArray()) {
                if (escaped) {
                    // Check if escape sequence is valid
                    if (!isValidEscapeChar(c)) {
                        throw new IllegalArgumentException("Invalid escape sequence: \\" + c);
                    }
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                }
            }

            // Check for trailing backslash (invalid escape)
            if (escaped) {
                throw new IllegalArgumentException("Invalid escape sequence: trailing backslash");
            }
        }
    }

    /**
     * Checks if a character is a valid escape sequence.
     */
    private static boolean isValidEscapeChar(final char c) {
        return c == 'n' || c == 'r' || c == 't' || c == '"' || c == '\\';
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

        for (char c : unquoted.toCharArray()) {
            if (escaped) {
                result.append(unescapeChar(c));
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Converts an escaped character to its unescaped form.
     *
     * @param c The character following a backslash
     * @return The unescaped character
     */
    private static char unescapeChar(final char c) {
        return switch (c) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case '"' -> '"';
            case '\\' -> '\\';
            default -> c;
        };
    }
}
