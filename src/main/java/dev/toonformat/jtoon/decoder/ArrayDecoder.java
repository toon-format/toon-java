package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.util.Headers;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import static dev.toonformat.jtoon.util.Constants.BACKSLASH;
import static dev.toonformat.jtoon.util.Constants.COLON;
import static dev.toonformat.jtoon.util.Constants.DOUBLE_QUOTE;
import static dev.toonformat.jtoon.util.Headers.ARRAY_HEADER_PATTERN;
import static dev.toonformat.jtoon.util.Headers.TABULAR_HEADER_PATTERN;

/**
 * Handles decoding of TOON arrays to JSON format.
 */
public final class ArrayDecoder {

    private static final int DELIMITER_GROUP_INDEX = 3;
    private static final int FIELDS_GROUP_INDEX = 4;

    private ArrayDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Spec §6: the delimiter declared inside the bracket segment of a tabular
     * header must match the delimiter used by the brace field list. A header
     * that declares a delimiter the field list does not use is defective.
     *
     * @param arrayHeader the array header starting with the bracket segment
     * @return true when the header carries a mismatched delimiter declaration
     */
    static boolean hasTabularDelimiterMismatch(final String arrayHeader) {
        final Matcher matcher = TABULAR_HEADER_PATTERN.matcher(arrayHeader);
        if (!matcher.find() || matcher.group(DELIMITER_GROUP_INDEX) == null) {
            return false;
        }
        final char declared = matcher.group(DELIMITER_GROUP_INDEX).charAt(0);
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = 0; i < matcher.group(FIELDS_GROUP_INDEX).length(); i++) {
            final char c = matcher.group(FIELDS_GROUP_INDEX).charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && c != declared && (c == ',' || c == '\t' || c == '|')) {
                return true;
            }
        }
        return false;
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
            rejectLengthMarker(tabularMatcher, context.options.strict());
            return TabularArrayDecoder.parseTabularArray(header, depth, arrayDelimiter, context);
        }

        if (arrayMatcher.find()) {
            rejectLengthMarker(arrayMatcher, context.options.strict());
            rejectLeadingZeroLength(arrayMatcher, context.options.strict());
            final int headerEndIdx = arrayMatcher.end();
            final String afterHeader = DecodeHelper.trimSpaces(header.substring(headerEndIdx));

            if (hasInlineContent(afterHeader)) {
                return parseInlineArray(afterHeader, header, arrayDelimiter, context);
            }

            // Spec §12: blank lines between the header and the first item are
            // accepted even in strict mode
            skipBlankLines(context);

            if (context.currentLine < context.lines.length) {
                return parseArrayDataLine(header, depth, context);
            }
            validateArrayLength(header, 0, context.options.maxArraySize(), context.options.strict());
            return Collections.unmodifiableList(new ArrayList<>());
        }

        // Spec §9.1/§9.2: a bare bracket pair is an empty array header
        if ("[]".equals(DecodeHelper.trimSpaces(header))) {
            context.currentLine++;
            return Collections.emptyList();
        }

        if (context.options.strict()) {
            throw new IllegalArgumentException("Invalid array header: " + header);
        }
        context.currentLine++;
        return Collections.emptyList();
    }

    /**
     * In strict mode, rejects the removed {@code [#N]} length marker (§6).
     *
     * @param headerMatcher the matched array or tabular header
     * @param strict        strict mode flag
     */
    private static void rejectLengthMarker(final Matcher headerMatcher, final boolean strict) {
        if (strict && !headerMatcher.group(1).isEmpty()) {
            throw new IllegalArgumentException("Invalid array header: " + headerMatcher.group());
        }
    }

    /**
     * In strict mode, rejects bracket lengths with leading zeros (e.g. [03])
     * unless the length is exactly "0".
     *
     * @param arrayMatcher the matched array header
     * @param strict       strict mode flag
     */
    private static void rejectLeadingZeroLength(final Matcher arrayMatcher, final boolean strict) {
        if (strict) {
            final String lengthStr = arrayMatcher.group(2);
            if (lengthStr.length() > 1 && lengthStr.charAt(0) == '0') {
                throw new IllegalArgumentException(
                    "Invalid array length with leading zeros: [" + lengthStr + "]");
            }
        }
    }

    /**
     * Returns whether the header text past the bracket segment declares
     * non-empty inline values ({@code header: v1,v2}).
     *
     * @param afterHeader the header text past the bracket segment
     * @return true when inline values follow the colon
     */
    private static boolean hasInlineContent(final String afterHeader) {
        return afterHeader.startsWith(COLON) && !DecodeHelper.trimSpaces(afterHeader.substring(1)).isEmpty();
    }

    /**
     * Parses the inline values of an array header ({@code header: v1,v2}).
     *
     * @param afterHeader    the header text past the bracket segment
     * @param header         the full header string
     * @param arrayDelimiter array delimiter
     * @param context        decode context
     * @return the parsed values
     */
    private static List<Object> parseInlineArray(final String afterHeader, final String header,
            final Delimiter arrayDelimiter, final DecodeContext context) {
        final String inlineContent = DecodeHelper.trimSpaces(afterHeader.substring(1));
        final List<Object> result = parseArrayValues(inlineContent, arrayDelimiter,
            context.options.maxArraySize(), context.options.maxStringLength());
        validateArrayLength(header, result.size(), context.options.maxArraySize(), context.options.strict());
        context.currentLine++;
        return Collections.unmodifiableList(result);
    }

    /**
     * Advances the current line past blank lines following the header.
     *
     * @param context decode context
     */
    private static void skipBlankLines(final DecodeContext context) {
        do {
            context.currentLine++;
        } while (context.currentLine < context.lines.length
            && DecodeHelper.isBlankLine(context.lines[context.currentLine]));
    }

    /**
     * Parses the first data line below an array header: a deeper list item
     * opens a list; any other line is left to the enclosing scope, so the
     * array is empty.
     *
     * @param header         the full header string
     * @param depth          depth of the array
     * @param context        decode context
     * @return the parsed array values
     */
    private static List<Object> parseArrayDataLine(final String header, final int depth,
            final DecodeContext context) {
        final String nextLine = context.lines[context.currentLine];
        final int nextDepth = DecodeHelper.getDepth(nextLine, context);
        final String nextContent = nextLine.substring(nextDepth * context.options.indent());

        if (nextDepth > depth && DecodeHelper.isListItemLine(nextContent)) {
            context.currentLine--;
            return Collections.unmodifiableList(parseListArray(depth, header, context));
        }

        validateArrayLength(header, 0, context.options.maxArraySize(), context.options.strict());
        return Collections.emptyList();
    }

    /**
     * Validates array length if declared in the header.
     * The count check applies in strict mode only; the declared length never
     * truncates a scope (§14.1), so non-strict mode ignores it entirely.
     *
     * @param header       header
     * @param actualLength actual length
     * @param maxArraySize maximum allowed array size
     * @param strict       strict mode flag
     */
    static void validateArrayLength(final String header, final int actualLength, final int maxArraySize,
            final boolean strict) {
        if (!strict) {
            return;
        }
        final Integer declaredLength = extractLengthFromHeader(header, maxArraySize);
        if (declaredLength != null && declaredLength != actualLength) {
            throw new IllegalArgumentException(
                String.format("Array length mismatch: declared %d, found %d", declaredLength, actualLength));
        }
    }

    /**
     * Extracts declared length from the array header with bounds checking.
     * Returns the number specified in [n] or null if not found.
     *
     * @param header      header string for length check
     * @param maxArraySize maximum allowed array size
     * @return extracted length from header, or null if not found
     */
    @Nullable
    private static Integer extractLengthFromHeader(final String header, final int maxArraySize) {
        final Matcher matcher = ARRAY_HEADER_PATTERN.matcher(header);
        if (matcher.find()) {
            final String lengthStr = matcher.group(2);
            final long longLength = Headers.parseLength(lengthStr);
            if (longLength > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                    "Array size too large: " + lengthStr);
            }
            if (longLength > maxArraySize) {
                throw new IllegalArgumentException(
                    "Array size " + longLength + " exceeds maximum allowed " + maxArraySize);
            }
            return (int) longLength;
        }
        return null;
    }

    static List<Object> parseArrayValues(final String values, final Delimiter arrayDelimiter,
                                          final int maxArraySize, final int maxStringLength) {
        final List<String> rawValues = parseDelimitedValues(values, arrayDelimiter);
        if (rawValues.size() > maxArraySize) {
            throw new IllegalArgumentException(
                "Array size " + rawValues.size() + " exceeds maximum allowed " + maxArraySize);
        }
        final List<Object> result = new ArrayList<>(rawValues.size());
        for (final String value : rawValues) {
            result.add(PrimitiveDecoder.parse(value, maxStringLength));
        }
        return result;
    }

    /**
     * Splits a string by delimiter, respecting quoted sections.
     * Spaces (U+0020 only) around delimiters are tolerated and trimmed (§12).
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
                final String value = DecodeHelper.trimSpaces(stringBuilder.toString());
                result.add(value);
                stringBuilder.setLength(0);
                i = skipSpaces(input, i + 1);
            } else {
                stringBuilder.append(currentChar);
                i++;
            }
        }

        // Add final value
        if (!stringBuilder.isEmpty() || input.endsWith(arrayDelimiter.toString())) {
            result.add(DecodeHelper.trimSpaces(stringBuilder.toString()));
        }

        return result;
    }

    /**
     * Returns the index of the first non-space character at or after the
     * given position. Only U+0020 is skipped – a tab may be the active
     * delimiter or part of the next token (§12).
     *
     * @param input the input string
     * @param start the position to scan from
     * @return the first non-space index, or the input length
     */
    private static int skipSpaces(final String input, final int start) {
        int i = start;
        while (i < input.length() && input.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    /**
     * Parses list an array format where items are prefixed with "- ".
     * Example: items[2]:\n - item1\n - item2
     */
    private static List<Object> parseListArray(final int depth, final String header, final DecodeContext context) {
        final List<Object> result = new ArrayList<>();
        context.currentLine++;
        final int firstItemLine = context.currentLine;
        final int itemDepth = DecodeHelper.findContentDepth(depth, context);

        boolean shouldContinue = true;
        while (shouldContinue && context.currentLine < context.lines.length) {
            final String line = context.lines[context.currentLine];

            if (DecodeHelper.isBlankLine(line)) {
                // Spec §12: blank lines between the header and the first item are
                // accepted even in strict mode
                if (result.isEmpty()) {
                    context.currentLine++;
                } else if (handleBlankLineInListArray(depth, context)) {
                    shouldContinue = false;
                }
            } else {
                final int lineDepth = DecodeHelper.getDepth(line, context);
                // A line between the header and an adopted deeper item depth belongs to no scope
                if (lineDepth > depth && lineDepth < itemDepth) {
                    DecodeHelper.processOverIndentedLine(context, lineDepth);
                } else if (shouldTerminateListArray(lineDepth, itemDepth - 1, line, context)) {
                    shouldContinue = false;
                } else {
                    ListItemDecoder.processListArrayItem(line, lineDepth, itemDepth - 1, result, context);
                }
            }
        }

        validateNoBlankLineInSpan(firstItemLine, context);
        if (header != null) {
            validateArrayLength(header, result.size(), context.options.maxArraySize(), context.options.strict());
        }
        return result;
    }

    /**
     * In strict mode, rejects a blank line between the first item line and
     * the last content line of a list. The items' nested scopes skip blank
     * lines on their own, so the whole span is checked once the list is
     * complete.
     *
     * @param firstItemLine the index of the first item line
     * @param context       decode an object to deal with lines, delimiter and options
     */
    private static void validateNoBlankLineInSpan(final int firstItemLine, final DecodeContext context) {
        if (!context.options.strict()) {
            return;
        }
        int lastLine = context.currentLine - 1;
        while (lastLine > firstItemLine && DecodeHelper.isBlankLine(context.lines[lastLine])) {
            lastLine--;
        }
        for (int i = firstItemLine; i < lastLine; i++) {
            if (DecodeHelper.isBlankLine(context.lines[i])) {
                throw new IllegalArgumentException("Blank line inside list array at line " + (i + 1));
            }
        }
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
        if (lineDepth == depth + 1) {
            final String content = line.substring((depth + 1) * context.options.indent());
            return !DecodeHelper.isListItemLine(content);
        }
        return false;
    }
}
