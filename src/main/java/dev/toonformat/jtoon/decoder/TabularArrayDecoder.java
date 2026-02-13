package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.util.StringEscaper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import static dev.toonformat.jtoon.util.Constants.BACKSLASH;
import static dev.toonformat.jtoon.util.Constants.DOUBLE_QUOTE;
import static dev.toonformat.jtoon.util.Headers.TABULAR_HEADER_PATTERN;

/**
 * Handles decoding of tabular arrays to JSON format.
 */
public final class TabularArrayDecoder {

    private TabularArrayDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parses tabular array format where each row contains delimiter-separated
     * values.
     * Example: items[2]{id,name}:\n 1,Ada\n 2,Bob
     *
     * @param header         the string representation of header
     * @param depth          depth of an array
     * @param arrayDelimiter the type of delimiter used in the array
     * @param context        decode an object to deal with lines, delimiter and options
     * @return tabular array converted to JSON format
     */
    public static List<Object> parseTabularArray(final String header, final int depth, final Delimiter arrayDelimiter,
                                                  final DecodeContext context) {
        final Matcher matcher = TABULAR_HEADER_PATTERN.matcher(header);
        if (!matcher.find()) {
            return Collections.emptyList();
        }

        final String keysStr = matcher.group(4);
        final List<String> keys = parseTabularKeys(keysStr, arrayDelimiter, context);

        final List<Object> result = new ArrayList<>();
        context.currentLine++;

        // Determine the expected row depth dynamically from the first non-blank line
        int expectedRowDepth = depth + 1;
        if (context.currentLine < context.lines.length) {
            final int nextNonBlankLine = DecodeHelper.findNextNonBlankLine(context.currentLine, context);
            if (nextNonBlankLine < context.lines.length) {
                expectedRowDepth = DecodeHelper.getDepth(context.lines[nextNonBlankLine], context);
            }
        }

        while (context.currentLine < context.lines.length) {
            if (!processTabularArrayLine(expectedRowDepth, keys, arrayDelimiter, result, context)) {
                break;
            }
        }

        ArrayDecoder.validateArrayLength(header, result.size());
        return result;
    }

    /**
     * Parses tabular header keys from field specification.
     * Validates delimiter consistency between bracket and brace fields.
     *
     * @param keysStr        the string representation of keys
     * @param arrayDelimiter the type of delimiter used in the array
     * @param context        decode an object to deal with lines, delimiter and options
     * @return list of keys
     */
    private static List<String> parseTabularKeys(final String keysStr, final Delimiter arrayDelimiter,
            final DecodeContext context) {
        // Validate delimiter mismatch between bracket and brace fields
        if (context.options.strict()) {
            validateKeysDelimiter(keysStr, arrayDelimiter);
        }

        final List<String> rawValues = ArrayDecoder.parseDelimitedValues(keysStr, arrayDelimiter);
        final List<String> result = new ArrayList<>(rawValues.size());
        for (final String key : rawValues) {
            result.add(StringEscaper.unescape(key));
        }
        return result;
    }

    /**
     * Validates delimiter consistency in tabular header keys.
     *
     * @param keysStr           the string representation of keys
     * @param expectedDelimiter the expected delimiter used in the array
     */
    private static void validateKeysDelimiter(final String keysStr, final Delimiter expectedDelimiter) {
        final char expectedChar = expectedDelimiter.toString().charAt(0);
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < keysStr.length(); i++) {
            final char c = keysStr.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == BACKSLASH) {
                escaped = true;
            } else if (c == DOUBLE_QUOTE) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                checkDelimiterMismatch(expectedChar, c);
            }
        }
    }

    /**
     * Checks for delimiter mismatch and throws an exception if found.
     *
     * @param expectedChar the expected delimiter character
     * @param actualChar   the actual delimiter character
     */
    private static void checkDelimiterMismatch(final char expectedChar, final char actualChar) {
        if (expectedChar == Delimiter.TAB.getValue() && actualChar == Delimiter.COMMA.getValue()) {
            throw new IllegalArgumentException("Delimiter mismatch: bracket declares tab (expected='"
                    + expectedChar + "', actual='" + actualChar + "')");
        }
        if (expectedChar == Delimiter.PIPE.getValue() && actualChar == Delimiter.COMMA.getValue()) {
            throw new IllegalArgumentException("Delimiter mismatch: bracket declares pipe (expected='"
                    + expectedChar + "', actual='" + actualChar + "')");
        }
        if (expectedChar == Delimiter.COMMA.getValue()
                && (actualChar == Delimiter.TAB.getValue() || actualChar == Delimiter.PIPE.getValue())) {
            throw new IllegalArgumentException(
                "Delimiter mismatch: bracket declares comma, brace fields use different delimiter");
        }
    }

    /**
     * Processes a single line in a tabular array.
     *
     * @param expectedRowDepth the expected depth of the next row
     * @param keys             the keys for the tabular array
     * @param arrayDelimiter   the type of delimiter used in the array
     * @param result           the list to store parsed rows in
     * @param context          decode an object to deal with lines, delimiter and options
     * @return true if parsing should continue, false if an array should terminate
     */
    private static boolean processTabularArrayLine(final int expectedRowDepth, final List<String> keys,
            final Delimiter arrayDelimiter, final List<Object> result,
            final DecodeContext context) {
        final String line = context.lines[context.currentLine];

        if (DecodeHelper.isBlankLine(line)) {
            return !handleBlankLineInTabularArray(expectedRowDepth, context);
        }

        final int lineDepth = DecodeHelper.getDepth(line, context);
        if (shouldTerminateTabularArray(line, lineDepth, expectedRowDepth, context)) {
            return false;
        }

        if (processTabularRow(line, lineDepth, expectedRowDepth, keys, arrayDelimiter, result, context)) {
            context.currentLine++;
        }
        return true;
    }

    /**
     * Handles blank line processing in a tabular array.
     *
     * @param expectedRowDepth the expected depth of the next row
     * @param context          decode an object to deal with lines, delimiter and options
     * @return true if an array should terminate, false if a line should be skipped
     */
    private static boolean handleBlankLineInTabularArray(final int expectedRowDepth, final DecodeContext context) {
        final int nextNonBlankLine = DecodeHelper.findNextNonBlankLine(context.currentLine + 1, context);

        if (nextNonBlankLine < context.lines.length) {
            final int nextDepth = DecodeHelper.getDepth(context.lines[nextNonBlankLine], context);
            // Header depth is one level above the expected row depth
            final int headerDepth = expectedRowDepth - 1;
            if (nextDepth <= headerDepth) {
                return true;
            }
        }

        // Blank line is inside the array
        if (context.options.strict()) {
            throw new IllegalArgumentException(
                "Blank line inside tabular array at line " + (context.currentLine + 1));
        }
        // In non-strict mode, skip blank lines
        context.currentLine++;
        return false;
    }

    /**
     * Determines if tabular array parsing should terminate based on online depth.
     *
     * @param line             the line to check
     * @param lineDepth        the depth of the line
     * @param expectedRowDepth the expected depth of the next row
     * @param context          decode an object to deal with lines, delimiter and options
     * @return true if an array should terminate, false otherwise.
     */
    private static boolean shouldTerminateTabularArray(final String line, final int lineDepth,
            final int expectedRowDepth, final DecodeContext context) {
        // Header depth is one level above the expected row depth
        final int headerDepth = expectedRowDepth - 1;

        if (lineDepth < expectedRowDepth) {
            if (lineDepth == headerDepth) {
                final String content = line.substring(headerDepth * context.options.indent());
                final int colonIdx = DecodeHelper.findUnquotedColon(content);
                if (colonIdx > 0) {
                    return true; // Key-value pair at the same depth-terminate an array
                }
            }
            return true; // Line depth is less than expected - terminate
        }

        // Check for a key-value pair at the expected row depth
        if (lineDepth == expectedRowDepth) {
            final String rowContent = line.substring(expectedRowDepth * context.options.indent());
            final int colonIdx = DecodeHelper.findUnquotedColon(rowContent);
            return colonIdx > 0; // Key-value pair at the same depth as rows - terminate an array
        }

        return false;
    }

    /**
     * Processes a tabular row if it matches the expected depth.
     *
     * @param line             the line to process
     * @param lineDepth        the depth of the line
     * @param expectedRowDepth the expected depth of the next row
     * @param keys             the keys for the tabular array
     * @param arrayDelimiter   the type of delimiter used in the array
     * @param result           the list to store parsed rows in
     * @param context          decode an object to deal with lines, delimiter and options
     * @return true if a line was processed and the currentLine should be incremented, false otherwise.
     */
    private static boolean processTabularRow(final String line, final int lineDepth,
            final int expectedRowDepth, final List<String> keys, final Delimiter arrayDelimiter,
            final List<Object> result, final DecodeContext context) {
        if (lineDepth == expectedRowDepth) {
            final String rowContent = line.substring(expectedRowDepth * context.options.indent());
            final Map<String, Object> row = parseTabularRow(rowContent, keys, arrayDelimiter, context);
            result.add(row);
            return true;
        } else if (lineDepth > expectedRowDepth) {
            // Line is deeper than expected - might be nested content, skip it
            context.currentLine++;
            return false;
        }
        return true;
    }

    /**
     * Parses a tabular row into a Map using the provided keys.
     * Validates that the row uses the correct delimiter.
     *
     * @param rowContent     the row content to parse
     * @param keys           the keys for the tabular array
     * @param arrayDelimiter the type of delimiter used in the array
     * @param context        decode an object to deal with lines, delimiter and options
     * @return a Map containing the parsed row values
     */
    private static Map<String, Object> parseTabularRow(final String rowContent, final List<String> keys,
                                                       final Delimiter arrayDelimiter, final DecodeContext context) {
        final Map<String, Object> row = new LinkedHashMap<>();
        final List<Object> values = ArrayDecoder.parseArrayValues(rowContent, arrayDelimiter);

        // Validate value count matches key count
        if (context.options.strict() && values.size() != keys.size()) {
            throw new IllegalArgumentException(
                String.format("Tabular row value count (%d) does not match header field count (%d)",
                              values.size(), keys.size()));
        }

        for (int i = 0; i < keys.size() && i < values.size(); i++) {
            row.put(keys.get(i), values.get(i));
        }

        return row;
    }
}
