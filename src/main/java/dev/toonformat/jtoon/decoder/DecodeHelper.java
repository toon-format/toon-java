package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.util.Headers;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Map;
import static dev.toonformat.jtoon.util.Constants.BACKSLASH;
import static dev.toonformat.jtoon.util.Constants.DOUBLE_QUOTE;
import static dev.toonformat.jtoon.util.Constants.SPACE;
import static dev.toonformat.jtoon.util.Constants.COLON;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_MARKER;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_PREFIX;

/**
 * Handles indentation, depth, conflicts, and validation for other decode classes.
 */
public final class DecodeHelper {

    private DecodeHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Trims surrounding spaces from a token – exactly U+0020, no other
     * characters (§12). Tabs, control characters, and NBSP stay part of the
     * token.
     *
     * @param token the raw token
     * @return the token without leading and trailing U+0020
     */
    static String trimSpaces(final String token) {
        int start = 0;
        int end = token.length();
        while (start < end && token.charAt(start) == ' ') {
            start++;
        }
        while (end > start && token.charAt(end - 1) == ' ') {
            end--;
        }
        return token.substring(start, end);
    }

    /**
     * Spec §7.4: after the closing quote of a quoted token only spaces
     * (U+0020, §12) may follow. A token without a leading quote passes; an
     * unterminated token is left to
     * {@link dev.toonformat.jtoon.util.StringEscaper#validateString}.
     *
     * @param token the token to validate
     * @throws IllegalArgumentException if another character follows the closing quote
     */
    static void validateQuotedTokenBoundary(final String token) {
        if (!token.startsWith("\"")) {
            return;
        }
        boolean escaped = false;
        for (int i = 1; i < token.length(); i++) {
            final char c = token.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                if (!trimSpaces(token.substring(i + 1)).isEmpty()) {
                    throw new IllegalArgumentException(
                        "Characters after closing quote in token: " + token);
                }
                return;
            }
        }
    }

    /**
     * Calculates indentation depth (nesting level) of a line.
     * Counts leading spaces in multiples of the configured indent size.
     * In strict mode, validates indentation (no tabs, proper multiples).
     *
     * @param line    the line string to parse
     * @param context decode an object to deal with lines, delimiter, and options
     * @return the depth of a line
     */
    public static int getDepth(final String line, final DecodeContext context) {
        // Blank lines (including lines with only spaces) have depth 0
        if (isBlankLine(line)) {
            return 0;
        }
        return computeLeadingSpaces(line, context) / Math.max(1, context.options.indent());
    }

    /**
     * Computes leading spaces, validates indentation in strict mode,
     * and rejects tabs. Single scan for all indentation logic.
     *
     * @param line    the line string to parse
     * @param context decode object in order to deal with lines, delimiter and options
     * @return amount of leading spaces
     */
    private static int computeLeadingSpaces(final String line, final DecodeContext context) {
        final int indentSize = context.options.indent();
        int leadingSpaces = 0;

        int i = 0;
        final int lengthOfLine = line.length();
        while (i < lengthOfLine) {
            final char c = line.charAt(i);
            if (c == SPACE.charAt(0)) {
                leadingSpaces++;
            } else if (c == Delimiter.TAB.getValue()) {
                if (context.options.strict()) {
                    throw new IllegalArgumentException(
                        "Tab character used in indentation at line " + (context.currentLine + 1));
                }
                // In non-strict mode treat tab as non-indent and stop.
                break;
            } else {
                break; // reached content
            }
            i++;
        }

        if (indentSize > 0 && leadingSpaces > 0 && leadingSpaces % indentSize != 0 && context.options.strict()) {
            throw new IllegalArgumentException(
                String.format("Non-multiple indentation: %d leadingSpaces with indent=%d at line %d",
                    leadingSpaces, indentSize, context.currentLine + 1));
        }

        return leadingSpaces;
    }


    /**
     * Checks if a line is blank (empty or only spaces).
     *
     * @param line the line string to parse
     * @return true or false depending on if the line is blank or not
     */
    static boolean isBlankLine(final String line) {
        return trimSpaces(line).isEmpty();
    }

    /**
     * Finds the index of the first unquoted colon in a line.
     * Critical for handling quoted keys like "order:id": value.
     *
     * @param content the content string to parse
     * @return the unquoted colon
     */
    static int findUnquotedColon(final String content) {
        return findUnquoted(content, COLON.charAt(0), 0);
    }

    /**
     * Finds the index of the first unquoted occurrence of a character.
     *
     * @param content the content string to scan
     * @param target  the character to find
     * @param from    the index to start scanning at
     * @return the index of the character, or -1 if absent
     */
    private static int findUnquoted(final String content, final char target, final int from) {
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = from; i < content.length(); i++) {
            final char c = content.charAt(i);

            if (c == target && !inQuotes) {
                return i;
            } else if (escaped) {
                escaped = false;
            } else if (inQuotes && c == BACKSLASH) {
                escaped = true;
            } else if (c == DOUBLE_QUOTE) {
                inQuotes = !inQuotes;
            }
        }

        return -1;
    }

    /**
     * Checks if content is a list-item line: the bare marker or the marker
     * followed by a space. A hyphen glued to its token is not a list item.
     *
     * @param content the line content past its indentation
     * @return true if the content is a list-item line
     */
    static boolean isListItemLine(final String content) {
        return LIST_ITEM_MARKER.equals(content) || content.startsWith(LIST_ITEM_PREFIX);
    }

    /**
     * Checks if content opens a keyless array: the bare {@code []}, or a
     * header that matches the §6 grammar. Any other bracket-led line is a
     * key-value line, which strict mode rejects for its malformed header
     * (§14.2), or a scalar line.
     *
     * @param content the line content past its indentation
     * @return true if the content is to be parsed as a keyless array
     */
    static boolean opensKeylessArray(final String content) {
        return "[]".equals(content) || Headers.matchKeylessKeyedHeader(content) != null;
    }

    /**
     * Finds the next non-blank line starting from the given index.
     *
     * @param startIndex given index
     * @param context    decode an object to deal with lines, delimiter, and options
     * @return index aiming for the next non-blank line
     */
    static int findNextNonBlankLine(final int startIndex, final DecodeContext context) {
        int index = startIndex;
        while (index < context.lines.length && isBlankLine(context.lines[index])) {
            index++;
        }
        return index;
    }

    /**
     * Finds the next non-blank line starting from the given index.
     *
     * @param finalSegment final segment
     * @param existing     existing
     * @param value        value present in a map
     * @param context      decode an object to deal with lines, delimiter, and options
     * @throws IllegalArgumentException in case there's a expansion conflict
     */
    static void checkFinalValueConflict(final String finalSegment, @Nullable final Object existing,
            final Object value, final DecodeContext context) {
        if (existing != null && context.options.strict()) {
            // Check for conflicts in strict mode
            if (existing instanceof Map && !(value instanceof Map)) {
                throw new IllegalArgumentException(
                    String.format("Path expansion conflict: %s is object, cannot set to %s",
                        finalSegment, value.getClass().getSimpleName()));
            }
            if (existing instanceof List && !(value instanceof List)) {
                throw new IllegalArgumentException(
                    String.format("Path expansion conflict: %s is array, cannot set to %s",
                        finalSegment, value.getClass().getSimpleName()));
            }
        }
    }

    /**
     * Checks for path expansion conflicts when setting a non-expanded key.
     * In strict mode, throws if the key conflicts with an existing expanded path.
     *
     * @param map     map
     * @param key     present the key in the map
     * @param value   present value in a map
     * @param context decode an object to deal with lines, delimiter, and options
     */
    static void checkPathExpansionConflict(final Map<String, Object> map, final String key,
            final Object value, final DecodeContext context) {
        if (!context.options.strict()) {
            return;
        }

        final Object existing = map.get(key);
        checkFinalValueConflict(key, existing, value, context);
    }

    /**
     * Checks for duplicate keys in strict mode.
     * Throws if the map already contains the given key and strict mode is enabled.
     *
     * @param map     the map to check
     * @param key     the key being inserted
     * @param context decode context for strict mode check
     * @throws IllegalArgumentException if strict mode and key already exists
     */
    static void checkDuplicateKey(final Map<String, Object> map, final String key, final DecodeContext context) {
        if (context.options.strict() && map.containsKey(key)) {
            throw new IllegalArgumentException(
                "Duplicate key '" + key + "' at line " + (context.currentLine + 1));
        }
    }

    /**
     * Finds the depth of the next non-blank line, skipping blank lines.
     *
     * @param context decode an object to deal with lines, delimiter and options
     * @return the depth of the next non-blank line, or null if none exists
     */
    static @Nullable Integer findNextNonBlankLineDepth(final DecodeContext context) {
        int nextLineIdx = context.currentLine;
        while (nextLineIdx < context.lines.length && isBlankLine(context.lines[nextLineIdx])) {
            nextLineIdx++;
        }

        if (nextLineIdx >= context.lines.length) {
            return null;
        }

        return getDepth(context.lines[nextLineIdx], context);
    }

    /**
     * Finds the content depth of a scope whose opening line sits at the given
     * depth: one level deeper, or in non-strict mode the depth of a deeper
     * first line, which later lines of the scope then have to match.
     *
     * @param openerDepth the depth of the line that opens the scope
     * @param context     decode an object to deal with lines, delimiter and options
     * @return the content depth of the scope
     */
    static int findContentDepth(final int openerDepth, final DecodeContext context) {
        final Integer firstDepth = findNextNonBlankLineDepth(context);
        if (!context.options.strict() && firstDepth != null && firstDepth > openerDepth + 1) {
            return firstDepth;
        }
        return openerDepth + 1;
    }

    /**
     * In strict mode, rejects a key-value line that still has a header shape:
     * an unquoted bracket segment in its key, followed by a colon past the
     * segment and its field list. The line did not match the header grammar,
     * so the header is malformed (§6, §14.2). This catches:
     * <ul>
     * <li>the removed length marker ({@code xs[#2]})</li>
     * <li>extra brackets between bracket segment and colon ({@code foo[1][bar]})</li>
     * <li>text between bracket segment and colon ({@code foo[2]extra})</li>
     * <li>noninteger bracket segment ({@code foo[bar]})</li>
     * <li>negative bracket length ({@code items[-1]})</li>
     * <li>whitespace between bracket segment and colon/fields segment
     * ({@code items[2] :}, {@code items[2] {a,b}:})</li>
     * <li>inline content after a field list ({@code items[1]{a}: 1})</li>
     * </ul>
     * A lone bracket ({@code a[b}) or a field list spanning the colon
     * ({@code [1]{x:y}}) leaves no header shape, so the line stays a
     * key-value line.
     *
     * @param key     the raw key token before the colon
     * @param value   the value after the colon
     * @param context decode an object to deal with lines, delimiter and options
     * @throws IllegalArgumentException in strict mode if the line has a header shape
     */
    static void rejectMalformedHeader(final String key, final String value, final DecodeContext context) {
        if (context.options.strict() && hasHeaderShape(key, value)) {
            throw new IllegalArgumentException(
                "Invalid array header syntax at line " + (context.currentLine + 1));
        }
    }

    private static boolean hasHeaderShape(final String key, final String value) {
        final int bracketStart = findUnquoted(key, '[', 0);
        if (bracketStart < 0) {
            return false;
        }
        final String content = key + COLON + value;
        final int bracketEnd = findUnquoted(content, ']', bracketStart);
        if (bracketEnd < 0) {
            return false;
        }
        int segmentEnd = bracketEnd;
        final int braceStart = findUnquoted(content, '{', bracketEnd);
        if (braceStart >= 0 && braceStart < findUnquoted(content, COLON.charAt(0), bracketEnd)) {
            segmentEnd = Math.max(segmentEnd, Headers.skipBalancedFieldSpec(content, braceStart + 1, content.length()));
        }
        return findUnquoted(content, COLON.charAt(0), segmentEnd) >= 0;
    }

    /**
     * Ensures no unconsumed lines remain after the root form was parsed.
     * The root form spans the whole document (§5); trailing content must not be
     * silently discarded. In strict mode any leftover line is an error. In
     * non-strict mode a scalar line outside root primitive position is still an
     * error in both modes alike (§5.2), while leftover key-value lines are ignored.
     *
     * @param context decode an object to deal with lines, delimiter and options
     */
    static void validateNoTrailingContent(final DecodeContext context) {
        while (context.currentLine < context.lines.length) {
            final String line = context.lines[context.currentLine];
            if (isBlankLine(line)) {
                context.currentLine++;
                continue;
            }
            if (context.options.strict()) {
                throw new IllegalArgumentException(
                    "Unexpected content after root form at line " + (context.currentLine + 1));
            }
            rejectScalarLine(context);
            context.currentLine++;
        }
    }

    /**
     * Rejects the current line if it is a scalar line: without an unquoted
     * colon it is a bare token outside root primitive position, an error in
     * strict and non-strict mode alike, so lenient skipping never covers it.
     *
     * @param context decode an object to deal with lines, delimiter and options
     * @throws IllegalArgumentException if the current line is a scalar line
     */
    private static void rejectScalarLine(final DecodeContext context) {
        if (findUnquotedColon(context.lines[context.currentLine]) < 0) {
            throw new IllegalArgumentException(
                "Bare token line outside root primitive position at line " + (context.currentLine + 1));
        }
    }

    /**
     * Skips or rejects a line that belongs to no scope (§8, §14.2).
     *
     * @param context   decode an object to deal with lines, delimiter, and options
     * @param lineDepth the depth of the over-indented line
     * @throws IllegalArgumentException in strict mode, or for a scalar line in any mode
     */
    static void processOverIndentedLine(final DecodeContext context, final int lineDepth) {
        if (context.options.strict()) {
            throw new IllegalArgumentException(
                "Over-indented line at " + (context.currentLine + 1) + " (depth " + lineDepth + ")");
        }
        rejectScalarLine(context);
        context.currentLine++;
    }

}
