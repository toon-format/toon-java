package dev.toonformat.jtoon.util;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Patterns in form of regex that must be followed in order to decode arrays, tabular, keyed arrays.
 */
public final class Headers {

    /**
     * Matches standalone array headers: [3], [3\t], [2|].
     * Group 1: optional # marker, captured so strict mode can reject it (§6),
     * Group 2: digits, Group 3: optional delimiter
     */
    public static final Pattern ARRAY_HEADER_PATTERN = Pattern.compile("^\\[(#?)(\\d+)([\\t|])?]");

    /**
     * Matches tabular array headers with field names: [2]{id,name,role}:.
     * Group 1: optional # marker, captured so strict mode can reject it (§6),
     * Group 2: digits, Group 3: optional delimiter, Group 4: field spec
     */
    public static final Pattern TABULAR_HEADER_PATTERN = Pattern.compile("^\\[(#?)(\\d+)([\\t|])?]\\{(.+)}:");

    /**
     * The keyed header scanner {@link #matchKeyedArrayHeader} replaces the
     * regex-based dispatch: a field spec may nest braces at any depth
     * ({@code {geo{point{lat,lon}}}}, §6, §9.3), which a flat regex cannot
     * express. The former KEYED_ARRAY_PATTERN is kept only for its key and
     * bracket-segment grammar, documented below. Its {@code [^\[\]:\s]++} key
     * alternative is the encoder form of §7.3 and is narrower than what
     * {@link #scanUnquotedKey} accepts: §7.4 requires decoders to accept any
     * unquoted key token, so this pattern is documentation, not the accepted
     * language.
     * Matches keyed array headers: items[2]{id,name}: or tags[3]:.
     * Group 1: key, Group 2: delimiter, Group 3: flat field spec.
     */
    public static final Pattern KEYED_ARRAY_PATTERN = Pattern.compile(
        "^(\"(?:[^\"\\\\]|\\\\.)*+\"|[^\\[\\]:\\s]++)\\[\\d++([\\t|])?](\\{[^}]+})?:.*+$");

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
        final int keyEnd = scanKey(content, n, requireKey);
        if (keyEnd < 0) {
            return null;
        }

        final BracketSegment bracket = scanBracketSegment(content, keyEnd, n);
        if (bracket == null) {
            return null;
        }

        final FieldSpecMatch fields = scanFieldSpec(content, bracket.endIndex(), n);
        if (fields == null) {
            return null;
        }

        // Trailing colon required after the bracket or field spec segment
        if (fields.endIndex() >= n || content.charAt(fields.endIndex()) != ':') {
            return null;
        }
        return new KeyedHeaderMatch(content.substring(0, keyEnd), keyEnd, bracket.declaredLength(),
            bracket.keyed(), bracket.delimiter(), fields.start(), fields.endIndex());
    }

    /**
     * Scans the key segment: a quoted key with escapes honored (§7.3), an
     * unquoted {@code [^\[\]:\s]+} key, or no key at all for keyless headers.
     *
     * @param content    the line content to scan
     * @param n          the content length
     * @param requireKey whether a key must be present
     * @return the index just past the key, or -1 when the key is missing or
     *         a quoted key is unterminated
     */
    private static int scanKey(final String content, final int n, final boolean requireKey) {
        if (!requireKey) {
            return 0;
        }
        if (content.charAt(0) == '"') {
            return scanQuotedKey(content, n);
        }
        return scanUnquotedKey(content, 0, n);
    }

    /**
     * Scans a quoted key up to its unescaped closing quote.
     *
     * @param content the line content to scan
     * @param n       the content length
     * @return the index just past the closing quote, or -1 when unterminated
     */
    private static int scanQuotedKey(final String content, final int n) {
        int i = 1;
        boolean escaped = false;
        while (i < n) {
            final char c = content.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return i + 1;
            }
            i++;
        }
        return -1;
    }

    /**
     * Scans an unquoted key up to a structural character.
     *
     * <p>Spec §7.4: an unquoted key token is the text before a header's bracket
     * segment, and decoders must accept any such token as a literal key. A
     * space inside the key is therefore part of the key; only U+0020 directly
     * before the bracket segment is the header syntax error of §6, and that is
     * rejected here so the line falls through to key-value parsing (§14.2).
     * Every other whitespace character stays part of the key (§12).
     *
     * @param content  the line content to scan
     * @param keyStart the index where the key starts
     * @param n        the content length
     * @return the index just past the key, or -1 when the key is empty, holds
     *         an unclosed quote, or a space separates it from the bracket segment
     */
    private static int scanUnquotedKey(final String content, final int keyStart, final int n) {
        int i = keyStart;
        boolean inQuotes = false;
        while (i < n) {
            final char c = content.charAt(i);
            if (inQuotes && c == '\\' && i + 1 < n) {
                i += 2;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && (c == '[' || c == ':')) {
                break;
            }
            i++;
        }
        // An unclosed quote opens a span to the end of the line, so no bracket segment follows (§5.2)
        if (inQuotes) {
            return -1;
        }
        if (i == keyStart) {
            return -1;
        }
        if (i < n && content.charAt(i) == '[' && content.charAt(i - 1) == ' ') {
            return -1;
        }
        return i;
    }

    /**
     * Scans the bracket segment: {@code [ \d+ (:)? ([\t|])? ]}.
     *
     * @param content the line content to scan
     * @param start   the index of the opening bracket
     * @param n       the content length
     * @return the parsed segment, or null for a malformed segment
     */
    @Nullable
    private static BracketSegment scanBracketSegment(final String content, final int start, final int n) {
        int i = start;
        if (i >= n || content.charAt(i) != '[') {
            return null;
        }
        i++;
        final int digitsStart = i;
        while (i < n && content.charAt(i) >= '0' && content.charAt(i) <= '9') {
            i++;
        }
        // A length has at least one ASCII digit and no leading zero (§6)
        if (i == digitsStart || (i - digitsStart > 1 && content.charAt(digitsStart) == '0')) {
            return null;
        }
        final long declaredLength = parseLength(content.substring(digitsStart, i));
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
        return new BracketSegment(declaredLength, keyed, delimiter, i + 1);
    }

    /**
     * Parses the digits of a bracket-segment length. A length beyond
     * {@code long} still forms a header, so it saturates to
     * {@link Long#MAX_VALUE}, a count no scope can meet.
     *
     * @param digits the length digits
     * @return the declared length
     */
    public static long parseLength(final String digits) {
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Scans the optional balanced field spec {@code {…}} with braces nested
     * at any depth; at least one field entry is required (§6).
     *
     * @param content the line content to scan
     * @param start   the index where the field spec may start
     * @param n       the content length
     * @return the parsed segment, or null for a malformed field spec
     */
    @Nullable
    private static FieldSpecMatch scanFieldSpec(final String content, final int start, final int n) {
        if (start >= n || content.charAt(start) != '{') {
            return new FieldSpecMatch(-1, start);
        }
        final int closingBrace = skipBalancedFieldSpec(content, start + 1, n);
        if (closingBrace < 0 || closingBrace - start <= 1) {
            return null;
        }
        return new FieldSpecMatch(start, closingBrace + 1);
    }

    /**
     * Skips a balanced brace group, honoring quoted field names and escaped
     * characters, and returns the index of its closing brace.
     *
     * @param content the line content to scan
     * @param i       the index just past the opening brace
     * @param n       the content length
     * @return the index of the matching closing brace, or -1 when unbalanced
     */
    public static int skipBalancedFieldSpec(final String content, final int i, final int n) {
        int pos = i;
        int depth = 1;
        boolean escaped = false;
        boolean inQuotes = false;
        while (pos < n) {
            final char c = content.charAt(pos);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && (c == '{' || c == '}')) {
                depth = adjustDepth(depth, c);
                if (depth == 0) {
                    return pos;
                }
            }
            pos++;
        }
        return -1;
    }

    /**
     * Adjusts the brace depth for an opening or closing brace.
     *
     * @param depth the current brace depth
     * @param c     the brace character
     * @return the adjusted depth
     */
    private static int adjustDepth(final int depth, final char c) {
        return c == '}' ? depth - 1 : depth + 1;
    }

    /**
     * The parsed bracket segment of a keyed header.
     *
     * @param declaredLength the declared entry/row count
     * @param keyed          whether the segment declares a keyed marker (§9.5)
     * @param delimiter      the declared delimiter, or null for the default
     * @param endIndex       the index just past the closing bracket
     */
    private record BracketSegment(long declaredLength, boolean keyed,
            @Nullable Character delimiter, int endIndex) {
    }

    /**
     * The parsed field spec segment of a keyed header.
     *
     * @param start    the index of the opening brace, or -1 without a field spec
     * @param endIndex the index just past the closing brace, or just past the
     *                 preceding segment without a field spec
     */
    private record FieldSpecMatch(int start, int endIndex) {
    }

    private Headers() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

}
