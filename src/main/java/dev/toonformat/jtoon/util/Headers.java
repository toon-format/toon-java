package dev.toonformat.jtoon.util;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Patterns in form of regex that must be followed in order to decode arrays, tabular, keyed arrays.
 */
public final class Headers {

    /**
     * Matches standalone array headers: [3], [#2], [3\t], [2|].
     * Group 1: optional # marker, Group 2: digits, Group 3: optional delimiter
     */
    public static final Pattern ARRAY_HEADER_PATTERN = Pattern.compile("^\\[(#?)(\\d+)([\\t|])?]");

    /**
     * Matches tabular array headers with field names: [2]{id,name,role}:.
     * Group 1: optional # marker, Group 2: digits, Group 3: optional delimiter,
     * Group 4: field spec
     */
    public static final Pattern TABULAR_HEADER_PATTERN = Pattern.compile("^\\[(#?)(\\d+)([\\t|])?]\\{(.+)}:");

    /**
     * The keyed header scanner {@link #matchKeyedArrayHeader} replaces the
     * regex-based dispatch: a field spec may nest braces at any depth
     * ({@code {geo{point{lat,lon}}}}, §6, §9.3), which a flat regex cannot
     * express. The former KEYED_ARRAY_PATTERN is kept only for its key and
     * bracket-segment grammar, documented below.
     * Matches keyed array headers: items[2]{id,name}: or tags[3]:.
     * Group 1: key, Group 2: #marker, Group 3: delimiter, Group 4: flat field spec.
     */
    public static final Pattern KEYED_ARRAY_PATTERN = Pattern.compile(
        "^(\"(?:[^\"\\\\]|\\\\.)*+\"|[^\\[\\]:\\s]++)\\[(#?)\\d++([\\t|])?](\\{[^}]+})?:.*+$");

    /**
     * Result of {@link #matchKeyedArrayHeader} and {@link #matchKeylessKeyedHeader}:
     * the matched key, the structural positions of the header segments, and the
     * declarations extracted from the bracket segment.
     *
     * @param key            the matched key, quoted or unquoted; empty for a keyless header
     * @param keyEnd         the index just past the key, where the bracket segment starts
     * @param declaredLength the declared entry/row count inside the bracket segment
     * @param keyed          whether the bracket segment declares a keyed marker (§9.5)
     * @param delimiter      the delimiter declared inside the bracket segment, or null for the default
     * @param fieldsStart    the index of the field-spec opening brace, or -1 without a field spec
     * @param headerEnd      the index just past the field spec, or just past the bracket segment
     */
    public record KeyedHeaderMatch(String key, int keyEnd, long declaredLength, boolean keyed,
            @Nullable Character delimiter, int fieldsStart, int headerEnd) {
    }

    /**
     * Scans a line for a keyed header ({@code key[N<delim?>]{field…}: …})
     * per the §6 grammar, accepting field specs with braces nested at any
     * depth. Braces, delimiters and colons inside quoted field names do not
     * count as structure. The bracket segment may declare a keyed marker
     * ({@code [N:]} or {@code [N:delim]}, §9.5).
     *
     * @param content the line content to scan
     * @return the keyed header match, or null when the content is not a keyed header
     */
    @Nullable
    public static KeyedHeaderMatch matchKeyedArrayHeader(final String content) {
        return scanHeader(content, true);
    }

    /**
     * Scans a keyless header that starts with a bracket segment, for
     * root-form discovery (§5). The match is returned whether or not it
     * declares a keyed marker; callers must test {@code keyed()} to tell
     * {@code [2:]{…}:} (keyed tabular object, §9.5) from {@code [2]{…}:}
     * (plain keyless tabular array).
     *
     * @param content the line content to scan
     * @return the keyless header match, or null when the content is not a keyless header
     */
    @Nullable
    public static KeyedHeaderMatch matchKeylessKeyedHeader(final String content) {
        return scanHeader(content, false);
    }

    @Nullable
    private static KeyedHeaderMatch scanHeader(final String content, final boolean requireKey) {
        final int n = content.length();
        int i = 0;

        if (requireKey) {
            // Key: quoted (escapes honored) or unquoted [^\[\]:\s]+ (§7.3)
            if (i < n && content.charAt(i) == '"') {
                i++;
                boolean escaped = false;
                boolean closed = false;
                while (i < n) {
                    final char c = content.charAt(i);
                    if (escaped) {
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        closed = true;
                        i++;
                        break;
                    }
                    i++;
                }
                if (!closed) {
                    return null;
                }
            } else {
                final int keyStart = i;
                while (i < n && content.charAt(i) != '['
                        && content.charAt(i) != ':' && !Character.isWhitespace(content.charAt(i))) {
                    i++;
                }
                if (i == keyStart) {
                    return null;
                }
            }
        }
        final int keyEnd = i;

        // Bracket segment: [ (#?) \d+ (:)? ([\t|])? ]
        if (i >= n || content.charAt(i) != '[') {
            return null;
        }
        i++;
        if (i < n && content.charAt(i) == '#') {
            i++;
        }
        final int digitsStart = i;
        while (i < n && Character.isDigit(content.charAt(i))) {
            i++;
        }
        if (i == digitsStart) {
            return null;
        }
        final long declaredLength;
        try {
            declaredLength = Long.parseLong(content.substring(digitsStart, i));
        } catch (NumberFormatException e) {
            return null;
        }
        boolean keyed = false;
        if (i < n && content.charAt(i) == ':') {
            keyed = true;
            i++;
        }
        @Nullable Character delimiter = null;
        if (i < n && (content.charAt(i) == '\t' || content.charAt(i) == '|')) {
            delimiter = content.charAt(i);
            i++;
        }
        if (i >= n || content.charAt(i) != ']') {
            return null;
        }
        i++;

        // Optional balanced field spec {…}, at least one field entry (§6)
        int fieldsStart = -1;
        if (i < n && content.charAt(i) == '{') {
            fieldsStart = i;
            int depth = 0;
            boolean escaped = false;
            boolean inQuotes = false;
            while (i < n) {
                final char c = content.charAt(i);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (!inQuotes && c == '{') {
                    depth++;
                } else if (!inQuotes && c == '}') {
                    depth--;
                    if (depth == 0) {
                        i++;
                        break;
                    }
                }
                i++;
            }
            if (depth != 0 || i - fieldsStart <= 2) {
                return null;
            }
        }

        if (i >= n || content.charAt(i) != ':') {
            return null;
        }
        return new KeyedHeaderMatch(content.substring(0, keyEnd), keyEnd, declaredLength, keyed, delimiter,
                fieldsStart, i);
    }

    private Headers() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

}
