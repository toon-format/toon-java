package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.util.StringEscaper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static dev.toonformat.jtoon.util.Headers.TABULAR_HEADER_PATTERN;

public class TabularArrayDecoder {

    private TabularArrayDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parses tabular array format where each row contains delimiter-separated
     * values.
     * Example: items[2]{id,name}:\n 1,Ada\n 2,Bob
     */
    public static List<Object> parseTabularArray(String header, int depth, String arrayDelimiter, DecodeContext context) {
        Matcher matcher = TABULAR_HEADER_PATTERN.matcher(header);
        if (!matcher.find()) {
            return new ArrayList<>();
        }

        String keysStr = matcher.group(4);
        List<String> keys = parseTabularKeys(keysStr, arrayDelimiter, context);

        List<Object> result = new ArrayList<>();
        context.currentLine++;

        // Determine the expected row depth dynamically from the first non-blank line
        int expectedRowDepth;
        if (context.currentLine < context.lines.length) {
            int nextNonBlankLine = DecodeHelper.findNextNonBlankLine(context.currentLine, context);
            if (nextNonBlankLine < context.lines.length) {
                expectedRowDepth = DecodeHelper.getDepth(context.lines[nextNonBlankLine], context);
            } else {
                expectedRowDepth = depth + 1;
            }
        } else {
            expectedRowDepth = depth + 1;
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
     */
    private static List<String> parseTabularKeys(String keysStr, String arrayDelimiter, DecodeContext context) {
        // Validate delimiter mismatch between bracket and brace fields
        if (context.options.strict()) {
            validateKeysDelimiter(keysStr, arrayDelimiter);
        }

        List<String> result = new ArrayList<>();
        List<String> rawValues = ArrayDecoder.parseDelimitedValues(keysStr, arrayDelimiter);
        for (String key : rawValues) {
            result.add(StringEscaper.unescape(key));
        }
        return result;
    }

    /**
     * Validates delimiter consistency in tabular header keys.
     */
    private static void validateKeysDelimiter(String keysStr, String expectedDelimiter) {
        char expectedChar = expectedDelimiter.charAt(0);
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < keysStr.length(); i++) {
            char c = keysStr.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                checkDelimiterMismatch(expectedChar, c);
            }
        }
    }

    /**
     * Checks for delimiter mismatch and throws an exception if found.
     */
    private static void checkDelimiterMismatch(char expectedChar, char actualChar) {
        if (expectedChar == '\t' && actualChar == ',') {
            throw new IllegalArgumentException(
                "Delimiter mismatch: bracket declares tab, brace fields use comma");
        }
        if (expectedChar == '|' && actualChar == ',') {
            throw new IllegalArgumentException(
                "Delimiter mismatch: bracket declares pipe, brace fields use comma");
        }
        if (expectedChar == ',' && (actualChar == '\t' || actualChar == '|')) {
            throw new IllegalArgumentException(
                "Delimiter mismatch: bracket declares comma, brace fields use different delimiter");
        }
    }

    /**
     * Processes a single line in a tabular array.
     * Returns true if parsing should continue, false if an array should terminate.
     */
    private static boolean processTabularArrayLine(int expectedRowDepth, List<String> keys, String arrayDelimiter,
                                                   List<Object> result, DecodeContext context) {
        String line = context.lines[context.currentLine];

        if (DecodeHelper.isBlankLine(line)) {
            return !handleBlankLineInTabularArray(expectedRowDepth, context);
        }

        int lineDepth = DecodeHelper.getDepth(line, context);
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
     * Returns true if an array should terminate, false if a line should be skipped.
     */
    private static boolean handleBlankLineInTabularArray(int expectedRowDepth, DecodeContext context) {
        int nextNonBlankLine = DecodeHelper.findNextNonBlankLine(context.currentLine + 1, context);

        if (nextNonBlankLine < context.lines.length) {
            int nextDepth = DecodeHelper.getDepth(context.lines[nextNonBlankLine], context);
            // Header depth is one level above the expected row depth
            int headerDepth = expectedRowDepth - 1;
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
     * Determines if tabular array parsing should terminate based online depth.
     * Returns true if array should terminate, false otherwise.
     */
    private static boolean shouldTerminateTabularArray(String line, int lineDepth, int expectedRowDepth, DecodeContext context) {
        // Header depth is one level above expected row depth
        int headerDepth = expectedRowDepth - 1;

        if (lineDepth < expectedRowDepth) {
            if (lineDepth == headerDepth) {
                String content = line.substring(headerDepth * context.options.indent());
                int colonIdx = DecodeHelper.findUnquotedColon(content);
                if (colonIdx > 0) {
                    return true; // Key-value pair at same depth - terminate array
                }
            }
            return true; // Line depth is less than expected - terminate
        }

        // Check for key-value pair at expected row depth
        if (lineDepth == expectedRowDepth) {
            String rowContent = line.substring(expectedRowDepth * context.options.indent());
            int colonIdx = DecodeHelper.findUnquotedColon(rowContent);
            return colonIdx > 0; // Key-value pair at same depth as rows - terminate array
        }

        return false;
    }

    /**
     * Processes a tabular row if it matches the expected depth.
     * Returns true if line was processed and currentLine should be incremented,
     * false otherwise.
     */
    private static boolean processTabularRow(String line, int lineDepth, int expectedRowDepth, List<String> keys,
                                             String arrayDelimiter, List<Object> result, DecodeContext context) {
        if (lineDepth == expectedRowDepth) {
            String rowContent = line.substring(expectedRowDepth * context.options.indent());
            Map<String, Object> row = parseTabularRow(rowContent, keys, arrayDelimiter, context);
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
     */
    private static Map<String, Object> parseTabularRow(String rowContent, List<String> keys, String arrayDelimiter, DecodeContext context) {
        Map<String, Object> row = new LinkedHashMap<>();
        List<Object> values = ArrayDecoder.parseArrayValues(rowContent, arrayDelimiter);

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
