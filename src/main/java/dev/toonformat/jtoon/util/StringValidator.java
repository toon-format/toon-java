package dev.toonformat.jtoon.util;

import java.util.regex.Pattern;

import static dev.toonformat.jtoon.util.Constants.*;

/**
 * Validates strings for safe unquoted usage in TOON format.
 * Follows Object Calisthenics principles with guard clauses and single-level
 * indentation.
 */
public final class StringValidator {
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?(?:e[+-]?\\d+)?$",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern OCTAL_PATTERN = Pattern.compile("^0[0-7]+$");
    private static final Pattern LEADING_ZERO_PATTERN = Pattern.compile("^0\\d+$");
    private static final Pattern UNQUOTED_KEY_PATTERN = Pattern.compile("^[A-Z_][\\w.]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STRUCTURAL_CHARS = Pattern.compile("[\\[\\]{}]");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\n\\r\\t]");

    private StringValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Checks if a string can be safely written without quotes.
     * Uses guard clauses and early returns for clarity.
     *
     * @param value     the string value to check
     * @param delimiter the delimiter being used (for validation)
     * @return true if the string can be safely written without quotes, false otherwise
     */
    public static boolean isSafeUnquoted(String value, String delimiter) {
        if (isNullOrEmpty(value)) {
            return false;
        }

        if (isPaddedWithWhitespace(value)) {
            return false;
        }

        if (looksLikeKeyword(value)) {
            return false;
        }

        if (looksLikeNumber(value)) {
            return false;
        }

        if (containsColon(value)) {
            return false;
        }

        if (containsQuotesOrBackslash(value)) {
            return false;
        }

        if (containsStructuralCharacters(value)) {
            return false;
        }

        if (containsControlCharacters(value)) {
            return false;
        }

        if (containsDelimiter(value, delimiter)) {
            return false;
        }

        return !startsWithListMarker(value);
    }

    /**
     * Checks if a key can be used without quotes.
     *
     * @param key the key to validate
     * @return true if the key can be used without quotes, false otherwise
     */
    public static boolean isValidUnquotedKey(String key) {
        return UNQUOTED_KEY_PATTERN.matcher(key).matches();
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static boolean isPaddedWithWhitespace(String value) {
        return !value.equals(value.trim());
    }

    private static boolean looksLikeKeyword(String value) {
        return value.equals(TRUE_LITERAL)
            || value.equals(FALSE_LITERAL)
            || value.equals(NULL_LITERAL);
    }

    private static boolean looksLikeNumber(String value) {
        return OCTAL_PATTERN.matcher(value).matches() || LEADING_ZERO_PATTERN.matcher(value).matches() || NUMERIC_PATTERN.matcher(value).matches();
    }

    private static boolean containsColon(String value) {
        return value.contains(COLON);
    }

    static boolean containsQuotesOrBackslash(String value) {
        return value.indexOf(DOUBLE_QUOTE) >= 0
            || value.indexOf(BACKSLASH) >= 0;
    }

    private static boolean containsStructuralCharacters(String value) {
        return STRUCTURAL_CHARS.matcher(value).find();
    }

    private static boolean containsControlCharacters(String value) {
        return CONTROL_CHARS.matcher(value).find();
    }

    private static boolean containsDelimiter(String value, String delimiter) {
        return value.contains(delimiter);
    }

    private static boolean startsWithListMarker(String value) {
        return value.startsWith(LIST_ITEM_MARKER);
    }
}
