package dev.toonformat.jtoon.decoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import static dev.toonformat.jtoon.util.Headers.ARRAY_HEADER_PATTERN;
import static dev.toonformat.jtoon.util.Headers.TABULAR_HEADER_PATTERN;

/**
 * Handles decoding of TOON arrays to JSON format.
 */
public class ArrayDecoder {

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
    protected static List<Object> parseArray(String header, int depth, DecodeContext context) {
        String arrayDelimiter = extractDelimiterFromHeader(header, context);

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
    protected static String extractDelimiterFromHeader(String header, DecodeContext context) {
        Matcher matcher = ARRAY_HEADER_PATTERN.matcher(header);
        if (matcher.find() && matcher.groupCount() == 3) {
            String delimiter = matcher.group(3);
            if (delimiter != null) {
                if ("\t".equals(delimiter)) {
                    return "\t";
                } else if ("|".equals(delimiter)) {
                    return "|";
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
    protected static List<Object> parseArrayWithDelimiter(String header, int depth, String arrayDelimiter, DecodeContext context) {
        Matcher tabularMatcher = TABULAR_HEADER_PATTERN.matcher(header);
        Matcher arrayMatcher = ARRAY_HEADER_PATTERN.matcher(header);

        if (tabularMatcher.find()) {
            return TabularArrayDecoder.parseTabularArray(header, depth, arrayDelimiter, context);
        }

        if (arrayMatcher.find()) {
            int headerEndIdx = arrayMatcher.end();
            String afterHeader = header.substring(headerEndIdx).trim();

            if (afterHeader.startsWith(":")) {
                String inlineContent = afterHeader.substring(1).trim();

                if (!inlineContent.isEmpty()) {
                    List<Object> result = parseArrayValues(inlineContent, arrayDelimiter);
                    validateArrayLength(header, result.size());
                    context.currentLine++;
                    return result;
                }
            }

            context.currentLine++;
            if (context.currentLine < context.lines.length) {
                String nextLine = context.lines[context.currentLine];
                int nextDepth = DecodeHelper.getDepth(nextLine, context);
                String nextContent = nextLine.substring(nextDepth * context.options.indent());

                if (nextDepth <= depth) {
                    // The next line is not a child of this array,
                    // the array is empty
                    validateArrayLength(header, 0);
                    return Collections.emptyList();
                }

                if (nextContent.startsWith("- ")) {
                    context.currentLine--;
                    return parseListArray(depth, header, context);
                } else {
                    context.currentLine++;
                    List<Object> result = parseArrayValues(nextContent, arrayDelimiter);
                    validateArrayLength(header, result.size());
                    return result;
                }
            }
            List<Object> empty = new ArrayList<>();
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
    protected static void validateArrayLength(String header, int actualLength) {
        Integer declaredLength = extractLengthFromHeader(header);
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
     * @return extracted length from header
     */
    private static Integer extractLengthFromHeader(String header) {
        Matcher matcher = ARRAY_HEADER_PATTERN.matcher(header);
        if (matcher.find() && matcher.groupCount() > 2) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                return null;
            }
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
    protected static List<Object> parseArrayValues(String values, String arrayDelimiter) {
        List<Object> result = new ArrayList<>();
        List<String> rawValues = parseDelimitedValues(values, arrayDelimiter);
        for (String value : rawValues) {
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
    protected static List<String> parseDelimitedValues(String input, String arrayDelimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        char delimiterChar = arrayDelimiter.charAt(0);

        int i = 0;
        while (i < input.length()) {
            char currentChar = input.charAt(i);

            if (escaped) {
                stringBuilder.append(currentChar);
                escaped = false;
                i++;
            } else if (currentChar == '\\') {
                stringBuilder.append(currentChar);
                escaped = true;
                i++;
            } else if (currentChar == '"') {
                stringBuilder.append(currentChar);
                inQuotes = !inQuotes;
                i++;
            } else if (currentChar == delimiterChar && !inQuotes) {
                // Found delimiter - add stringBuilder value (trimmed) and reset
                String value = stringBuilder.toString().trim();
                result.add(value);
                stringBuilder = new StringBuilder();
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
        if (!stringBuilder.isEmpty() || input.endsWith(arrayDelimiter)) {
            result.add(stringBuilder.toString().trim());
        }

        return result;
    }

    /**
     * Parses list an array format where items are prefixed with "- ".
     * Example: items[2]:\n - item1\n - item2
     */
    private static List<Object> parseListArray(int depth, String header, DecodeContext context) {
        List<Object> result = new ArrayList<>();
        context.currentLine++;

        boolean shouldContinue = true;
        while (shouldContinue && context.currentLine < context.lines.length) {
            String line = context.lines[context.currentLine];

            if (DecodeHelper.isBlankLine(line)) {
                if (handleBlankLineInListArray(depth, context)) {
                    shouldContinue = false;
                }
            } else {
                int lineDepth = DecodeHelper.getDepth(line, context);
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
    private static boolean handleBlankLineInListArray(int depth, DecodeContext context) {
        int nextNonBlankLine = DecodeHelper.findNextNonBlankLine(context.currentLine + 1, context);

        if (nextNonBlankLine >= context.lines.length) {
            return true; // EOF - terminate array
        }

        int nextDepth = DecodeHelper.getDepth(context.lines[nextNonBlankLine], context);
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
    private static boolean shouldTerminateListArray(int lineDepth, int depth, String line, DecodeContext context) {
        if (lineDepth < depth + 1) {
            return true; // Line depth is less than expected - terminate
        }
        // Also terminate if line is at expected depth but doesn't start with "-"
        if (lineDepth == depth + 1) {
            String content = line.substring((depth + 1) * context.options.indent());
            return !content.startsWith("-"); // Not an array item - terminate
        }
        return false;
    }
}
