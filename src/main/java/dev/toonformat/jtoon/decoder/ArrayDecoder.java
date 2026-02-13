package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.Delimiter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import static dev.toonformat.jtoon.util.Constants.BACKSLASH;
import static dev.toonformat.jtoon.util.Constants.COLON;
import static dev.toonformat.jtoon.util.Constants.DOUBLE_QUOTE;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_PREFIX;
import static dev.toonformat.jtoon.util.Headers.ARRAY_HEADER_PATTERN;
import static dev.toonformat.jtoon.util.Headers.TABULAR_HEADER_PATTERN;

/**
 * Handles decoding of TOON arrays to JSON format.
 */
public final class ArrayDecoder {

    private static final int DELIMITER_GROUP_INDEX = 3;

    private ArrayDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parses array from the header string and the following lines.
     * Detects array type (tabular, list, or primitive) and routes accordingly.
     *
     * @param header  the header string to parse
     * @param depth   the depth of an array
     * @param context decode an object to deal with lines, delimiter and options
     * @return parsed array with delimiter
     */
    static List<Object> parseArray(final String header, final int depth, final DecodeContext context) {
        final Delimiter arrayDelimiter = extractDelimiterFromHeader(header, context);

        return parseArrayWithDelimiter(header, depth, arrayDelimiter, context);
    }

    /**
     * Extracts delimiter from the array header.
     * Returns tab, pipe, or comma (default) based on a header pattern.
     *
     * @param header  the header string to parse
     * @param context decode an object to deal with lines, delimiter and options
     * @return extracted delimiter from header
     */
    static Delimiter extractDelimiterFromHeader(final String header, final DecodeContext context) {
        final Matcher matcher = ARRAY_HEADER_PATTERN.matcher(header);
        if (matcher.find()) {
            final String delimiter = matcher.group(DELIMITER_GROUP_INDEX);
            if (delimiter != null) {
                if (Delimiter.TAB.toString().equals(delimiter)) {
                    return Delimiter.TAB;
                }
                if (Delimiter.PIPE.toString().equals(delimiter)) {
                    return Delimiter.PIPE;
                }
            }
        }
        // Default to comma
        return context.delimiter;
    }

    /**
     * Parses array from the header string and following lines with a specific
     * delimiter.
     * Detects array type (tabular, list, or primitive) and routes accordingly.
     *
     * @param header         the header string to parse
     * @param depth          depth of an array
     * @param arrayDelimiter array delimiter
     * @param context        decode an object to deal with lines, delimiter and options
     * @return parsed array
     */
    static List<Object> parseArrayWithDelimiter(final String header, final int depth, final Delimiter arrayDelimiter,
                                                final DecodeContext context) {
        final Matcher tabularMatcher = TABULAR_HEADER_PATTERN.matcher(header);
        final Matcher arrayMatcher = ARRAY_HEADER_PATTERN.matcher(header);

        if (tabularMatcher.find()) {
            return TabularArrayDecoder.parseTabularArray(header, depth, arrayDelimiter, context);
        }

        if (arrayMatcher.find()) {
            final int headerEndIdx = arrayMatcher.end();
            final String afterHeader = header.substring(headerEndIdx).trim();

            if (afterHeader.startsWith(COLON)) {
                final String inlineContent = afterHeader.substring(1).trim();

                if (!inlineContent.isEmpty()) {
                    final List<Object> result = parseArrayValues(inlineContent, arrayDelimiter);
                    validateArrayLength(header, result.size());
                    context.currentLine++;
                    return result;
                }
            }

            context.currentLine++;
            if (context.currentLine < context.lines.length) {
                final String nextLine = context.lines[context.currentLine];
                final int nextDepth = DecodeHelper.getDepth(nextLine, context);
                final String nextContent = nextLine.substring(nextDepth * context.options.indent());

                if (nextDepth <= depth) {
                    // The next line is not a child of this array,
                    // the array is empty
                    validateArrayLength(header, 0);
                    return Collections.emptyList();
                }

                if (nextContent.startsWith(LIST_ITEM_PREFIX)) {
                    context.currentLine--;
                    return parseListArray(depth, header, context);
                } else {
                    context.currentLine++;
                    final List<Object> result = parseArrayValues(nextContent, arrayDelimiter);
                    validateArrayLength(header, result.size());
                    return result;
                }
            }
            final List<Object> empty = new ArrayList<>();
            validateArrayLength(header, 0);
            return empty;
        }

        if (context.options.strict()) {
            throw new IllegalArgumentException("Invalid array header: " + header);
        }
        return Collections.emptyList();
    }

    /**
     * Validates array length if declared in the header.
     *
     * @param header       header
     * @param actualLength actual length
     */
    static void validateArrayLength(final String header, final int actualLength) {
        final Integer declaredLength = extractLengthFromHeader(header);
        if (declaredLength != null && declaredLength != actualLength) {
            throw new IllegalArgumentException(
                String.format("Array length mismatch: declared %d, found %d", declaredLength, actualLength));
        }
    }

    /**
     * Extracts declared length from the array header.
     * Returns the number specified in [n] or null if not found.
     *
     * @param header header string for length check
     * @return extracted length from header, or null if not found
     */
    private static Integer extractLengthFromHeader(final String header) {
        final Matcher matcher = ARRAY_HEADER_PATTERN.matcher(header);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(2));
        }
        return null;
    }

    /**
     * Parses array values from a delimiter-separated string.
     *
     * @param values         the value string to parse
     * @param arrayDelimiter array delimiter
     * @return parsed array values
     */
    static List<Object> parseArrayValues(final String values, final Delimiter arrayDelimiter) {
        final List<String> rawValues = parseDelimitedValues(values, arrayDelimiter);
        final List<Object> result = new ArrayList<>(rawValues.size());
        for (final String value : rawValues) {
            result.add(PrimitiveDecoder.parse(value));
        }
        return result;
    }

    /**
     * Splits a string by delimiter, respecting quoted sections.
     * Whitespace around delimiters is tolerated and trimmed.
     *
     * @param input          the input string to parse
     * @param arrayDelimiter array delimiter
     * @return parsed delimited values
     */
    static List<String> parseDelimitedValues(final String input, final Delimiter arrayDelimiter) {
        final List<String> result = new ArrayList<>();
        final StringBuilder stringBuilder = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        final char delimiterChar = arrayDelimiter.toString().charAt(0);

        int i = 0;
        while (i < input.length()) {
            final char currentChar = input.charAt(i);

            if (escaped) {
                stringBuilder.append(currentChar);
                escaped = false;
                i++;
            } else if (currentChar == BACKSLASH) {
                stringBuilder.append(currentChar);
                escaped = true;
                i++;
            } else if (currentChar == DOUBLE_QUOTE) {
                stringBuilder.append(currentChar);
                inQuotes = !inQuotes;
                i++;
            } else if (currentChar == delimiterChar && !inQuotes) {
                // Found delimiter - add stringBuilder value (trimmed) and reset
                final String value = stringBuilder.toString().trim();
                result.add(value);
                stringBuilder.setLength(0);
                // Skip whitespace after delimiter
                do {
                    i++;
                } while (i < input.length() && Character.isWhitespace(input.charAt(i)));
            } else {
                stringBuilder.append(currentChar);
                i++;
            }
        }

        // Add final value
        if (!stringBuilder.isEmpty() || input.endsWith(arrayDelimiter.toString())) {
            result.add(stringBuilder.toString().trim());
        }

        return result;
    }

    /**
     * Parses list an array format where items are prefixed with "- ".
     * Example: items[2]:\n - item1\n - item2
     */
    private static List<Object> parseListArray(final int depth, final String header, final DecodeContext context) {
        final List<Object> result = new ArrayList<>();
        context.currentLine++;

        boolean shouldContinue = true;
        while (shouldContinue && context.currentLine < context.lines.length) {
            final String line = context.lines[context.currentLine];

            if (DecodeHelper.isBlankLine(line)) {
                if (handleBlankLineInListArray(depth, context)) {
                    shouldContinue = false;
                }
            } else {
                final int lineDepth = DecodeHelper.getDepth(line, context);
                if (shouldTerminateListArray(lineDepth, depth, line, context)) {
                    shouldContinue = false;
                } else {
                    ListItemDecoder.processListArrayItem(line, lineDepth, depth, result, context);
                }
            }
        }

        if (header != null) {
            validateArrayLength(header, result.size());
        }
        return result;
    }

    /**
     * Handles blank line processing in a list array.
     * Returns true if an array should terminate, false if a line should be skipped.
     *
     * @param depth   the depth of the blank line
     * @param context decode an object to deal with lines, delimiter and options
     * @return true if an array should terminate, false if a line should be skipped
     */
    private static boolean handleBlankLineInListArray(final int depth, final DecodeContext context) {
        final int nextNonBlankLine = DecodeHelper.findNextNonBlankLine(context.currentLine + 1, context);

        if (nextNonBlankLine >= context.lines.length) {
            return true; // EOF - terminate array
        }

        final int nextDepth = DecodeHelper.getDepth(context.lines[nextNonBlankLine], context);
        if (nextDepth <= depth) {
            return true; // Blank line is outside array - terminate
        }

        // Blank line is inside the array
        if (context.options.strict()) {
            throw new IllegalArgumentException("Blank line inside list array at line " + (context.currentLine + 1));
        }
        // In non-strict mode, skip blank lines
        context.currentLine++;
        return false;
    }

    /**
     * Determines if list array parsing should terminate based on online depth.
     *
     * @param lineDepth the depth of the line being parsed
     * @param depth     the depth of the array
     * @param context   decode an object to deal with lines, delimiter and options
     * @return true if an array should terminate, false otherwise.
     */
    private static boolean shouldTerminateListArray(final int lineDepth, final int depth,
            final String line, final DecodeContext context) {
        if (lineDepth < depth + 1) {
            return true; // Line depth is less than expected - terminate
        }
        // Also terminate if line is at expected depth but doesn't start with "-"
        if (lineDepth == depth + 1) {
            final String content = line.substring((depth + 1) * context.options.indent());
            return !content.startsWith("-"); // Not an array item - terminate
        }
        return false;
    }
}
