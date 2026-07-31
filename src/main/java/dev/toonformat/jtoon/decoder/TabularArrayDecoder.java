package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.util.StringEscaper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import static dev.toonformat.jtoon.util.Constants.BACKSLASH;
import static dev.toonformat.jtoon.util.Constants.DOUBLE_QUOTE;
import static dev.toonformat.jtoon.util.Headers.TABULAR_HEADER_PATTERN;

/**
 * Handles decoding of tabular arrays to JSON format.
 *
 * <p>In strict mode ({@code DecodeOptions.strict() == true}), each tabular row must contain exactly
 * the same number of values as the header declares field keys, or an
 * {@link IllegalArgumentException} is thrown.</p>
 *
 * <p>In lenient mode ({@code strict == false}), rows with fewer values than keys will have the
 * missing keys silently omitted, and rows with more values than keys will have the extra values
 * silently dropped. This means decoding can produce partial data without error when
 * strict validation is disabled.</p>
 */
public final class TabularArrayDecoder {

    private TabularArrayDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * One entry of a tabular header's field list (§6, §9.3). A leaf field
     * carries an empty child list; a field with a nested field group carries
     * its own ordered subfield list.
     *
     * @param name     the field name
     * @param children subfields of a nested field group, empty for a leaf field
     */
    record FieldNode(String name, List<FieldNode> children) {
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
        final List<FieldNode> fields = parseTabularKeys(keysStr, arrayDelimiter, context);

        // Spec §9.3: a duplicate field name within one field list is a header
        // defect, diagnosed from the header line alone. Names repeated at
        // different nesting levels are not duplicates.
        if (context.options.strict()) {
            validateNoDuplicateFields(fields, context);
        }

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
            if (!processTabularArrayLine(expectedRowDepth, fields, arrayDelimiter, result, context)) {
                break;
            }
        }

        ArrayDecoder.validateArrayLength(header, result.size(), context.options.maxArraySize(),
            context.options.strict());
        return Collections.unmodifiableList(result);
    }

    /**
     * Parses tabular header keys from field specification.
     * Validates delimiter consistency between bracket and brace fields.
     * Nested field groups ({@code field{sub1,sub2}}) become inner field nodes.
     *
     * @param keysStr        the string representation of keys
     * @param arrayDelimiter the type of delimiter used in the array
     * @param context        decode an object to deal with lines, delimiter and options
     * @return the parsed field tree
     */
    static List<FieldNode> parseTabularKeys(final String keysStr, final Delimiter arrayDelimiter,
            final DecodeContext context) {
        // Validate delimiter mismatch between bracket and brace fields
        if (context.options.strict()) {
            validateKeysDelimiter(keysStr, arrayDelimiter);
        }

        final List<FieldNode> result = new ArrayList<>();
        parseFieldList(keysStr, 0, arrayDelimiter, context, result);
        return result;
    }

    /**
     * Recursively parses a field list. Braces outside quoted names open a
     * nested field group parsed with the same delimiter (§6, §9.3).
     *
     * @param fieldList      the field list string to parse
     * @param start          the index at which parsing starts
     * @param arrayDelimiter the type of delimiter used in the array
     * @param context        decode an object to deal with lines, delimiter and options
     * @param result         the list to add parsed fields to
     * @return the index just past the closing brace of the parsed group, or -1
     *         when the string ends before a group is closed
     */
    private static int parseFieldList(final String fieldList, final int start, final Delimiter arrayDelimiter,
            final DecodeContext context, final List<FieldNode> result) {
        final char delimiterChar = arrayDelimiter.toString().charAt(0);
        final StringBuilder name = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        int i = start;
        while (i < fieldList.length()) {
            final char c = fieldList.charAt(i);
            if (escaped) {
                name.append(c);
                escaped = false;
                i++;
            } else if (c == BACKSLASH) {
                name.append(c);
                escaped = true;
                i++;
            } else if (c == DOUBLE_QUOTE) {
                name.append(c);
                inQuotes = !inQuotes;
                i++;
            } else if (!inQuotes && c == '{') {
                final List<FieldNode> children = new ArrayList<>();
                final int next = parseFieldList(fieldList, i + 1, arrayDelimiter, context, children);
                if (next < 0) {
                    if (context.options.strict()) {
                        throw new IllegalArgumentException(
                            "Unbalanced braces in tabular header field list");
                    }
                    i = fieldList.length();
                    continue;
                }
                result.add(new FieldNode(StringEscaper.unescape(name.toString().trim()), children));
                name.setLength(0);
                i = next;
            } else if (!inQuotes && c == '}') {
                flushField(result, name);
                return i + 1;
            } else if (!inQuotes && c == delimiterChar) {
                flushField(result, name);
                i++;
                while (i < fieldList.length() && Character.isWhitespace(fieldList.charAt(i))) {
                    i++;
                }
            } else {
                name.append(c);
                i++;
            }
        }
        flushField(result, name);
        return -1;
    }

    /**
     * Adds the buffered field name as a leaf node and resets the buffer.
     */
    private static void flushField(final List<FieldNode> result, final StringBuilder name) {
        if (!name.isEmpty()) {
            result.add(new FieldNode(StringEscaper.unescape(name.toString().trim()), Collections.emptyList()));
            name.setLength(0);
        }
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
     * @param fields           the field tree for the tabular array
     * @param arrayDelimiter   the type of delimiter used in the array
     * @param result           the list to store parsed rows in
     * @param context          decode an object to deal with lines, delimiter and options
     * @return true if parsing should continue, false if an array should terminate
     */
    private static boolean processTabularArrayLine(final int expectedRowDepth, final List<FieldNode> fields,
            final Delimiter arrayDelimiter, final List<Object> result,
            final DecodeContext context) {
        final String line = context.lines[context.currentLine];

        if (DecodeHelper.isBlankLine(line)) {
            // Spec §12: blank lines between the header and the first row are
            // accepted even in strict mode
            if (result.isEmpty()) {
                context.currentLine++;
                return true;
            }
            return !handleBlankLineInTabularArray(expectedRowDepth, context);
        }

        final int lineDepth = DecodeHelper.getDepth(line, context);
        if (shouldTerminateTabularArray(line, lineDepth, expectedRowDepth, context)) {
            return false;
        }

        if (processTabularRow(line, lineDepth, expectedRowDepth, fields, arrayDelimiter, result, context)) {
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

        if (nextNonBlankLine >= context.lines.length) {
            // Blank lines at the end of the document are trailing newlines (§12)
            return true;
        }
        final int nextDepth = DecodeHelper.getDepth(context.lines[nextNonBlankLine], context);
        // Header depth is one level above the expected row depth
        final int headerDepth = expectedRowDepth - 1;
        if (nextDepth <= headerDepth) {
            return true;
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
     * Implements the full disambiguation algorithm per spec §9.3:
     * - Compute the first unquoted occurrence of the active delimiter and the first unquoted colon.
     * - If a same-depth line has no unquoted colon → row.
     * - If both appear, compare first-unquoted positions:
     *   - Delimiter before colon → row.
     *   - Colon before delimiter → key-value line (end of rows).
     * - If a line has an unquoted colon but no unquoted active delimiter → key-value line.
     *
     * @param line             the line to check
     * @param lineDepth        the depth of the line
     * @param expectedRowDepth the expected depth of the next row
     * @param context          decode an object to deal with lines, delimiter and options
     * @return true if an array should terminate, false otherwise.
     */
    private static boolean shouldTerminateTabularArray(final String line, final int lineDepth,
            final int expectedRowDepth, final DecodeContext context) {
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

        if (lineDepth != expectedRowDepth) {
            return false;
        }

        // Spec §9.3 disambiguation at row depth
        final String rowContent = line.substring(expectedRowDepth * context.options.indent());
        final char delimChar = context.delimiter.getValue();
        final int delimIdx = findFirstUnquoted(rowContent, delimChar);
        final int colonIdx = DecodeHelper.findUnquotedColon(rowContent);

        if (colonIdx < 0) {
            return false; // No colon → this is a row
        }

        if (delimIdx < 0) {
            return true; // Colon present, no delimiter → key-value line
        }

        // Both colon and delimiter present: compare positions
        return colonIdx < delimIdx; // Colon first → key-value; delimiter first → row
    }

    /**
     * Finds the index of the first unquoted occurrence of a character in a string.
     */
    private static int findFirstUnquoted(final String content, final char target) {
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = 0; i < content.length(); i++) {
            final char c = content.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && c == target) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Processes a tabular row if it matches the expected depth.
     *
     * @param line             the line to process
     * @param lineDepth        the depth of the line
     * @param expectedRowDepth the expected depth of the next row
     * @param fields           the field tree for the tabular array
     * @param arrayDelimiter   the type of delimiter used in the array
     * @param result           the list to store parsed rows in
     * @param context          decode an object to deal with lines, delimiter and options
     * @return true if a line was processed and the currentLine should be incremented, false otherwise.
     */
    private static boolean processTabularRow(final String line, final int lineDepth,
            final int expectedRowDepth, final List<FieldNode> fields, final Delimiter arrayDelimiter,
            final List<Object> result, final DecodeContext context) {
        if (lineDepth == expectedRowDepth) {
            final String rowContent = line.substring(expectedRowDepth * context.options.indent());
            final Map<String, Object> row = parseTabularRow(rowContent, fields, arrayDelimiter, context);
            result.add(row);
            return true;
        } else if (lineDepth > expectedRowDepth) {
            // A line deeper than the row depth belongs to no scope (§14.2)
            if (context.options.strict()) {
                throw new IllegalArgumentException(
                    "Over-indented line after tabular rows at line " + (context.currentLine + 1));
            }
            // In non-strict mode, skip it
            context.currentLine++;
            return false;
        }
        return true;
    }

    /**
     * Parses a tabular row into a Map using the provided field tree.
     * A leaf field consumes the next cell; a nested field group materializes
     * an object from its subfields (§9.3).
     *
     * <p>In strict mode, the number of values must exactly match the leaf-field
     * count. In lenient mode, excess values are silently dropped and missing
     * values result in omitted keys.</p>
     *
     * @param rowContent     the row content to parse
     * @param fields         the field tree for the tabular array
     * @param arrayDelimiter the type of delimiter used in the array
     * @param context        decode an object to deal with lines, delimiter and options
     * @return a Map containing the parsed row values
     */
    static Map<String, Object> parseTabularRow(final String rowContent, final List<FieldNode> fields,
            final Delimiter arrayDelimiter, final DecodeContext context) {
        final Map<String, Object> row = new LinkedHashMap<>();
        final List<Object> values = ArrayDecoder.parseArrayValues(rowContent, arrayDelimiter,
            context.options.maxArraySize(), context.options.maxStringLength());

        // Spec §9.3: each row must carry exactly one cell per leaf field
        if (context.options.strict() && values.size() != countLeaves(fields)) {
            throw new IllegalArgumentException(
                String.format("Tabular row value count (%d) does not match header leaf-field count (%d)",
                              values.size(), countLeaves(fields)));
        }

        assignRowValues(fields, values, row, 0);

        return row;
    }

    /**
     * Assigns row cells to the field tree in depth-first, pre-order walk
     * order (§9.3): a leaf field takes the next cell, a nested field group
     * materializes an object from its subfields.
     */
    static void assignRowValues(final List<FieldNode> fields, final List<Object> values,
            final Map<String, Object> target, final int... nextCell) {
        for (final FieldNode field : fields) {
            if (field.children().isEmpty()) {
                final int index = nextCell[0];
                nextCell[0] = index + 1;
                if (index < values.size()) {
                    target.put(field.name(), values.get(index));
                }
            } else {
                final Map<String, Object> group = new LinkedHashMap<>();
                assignRowValues(field.children(), values, group, nextCell);
                target.put(field.name(), group);
            }
        }
    }

    /**
     * Counts the leaf fields of a field tree (§9.3): each row carries exactly
     * one cell per leaf field.
     */
    static int countLeaves(final List<FieldNode> fields) {
        int count = 0;
        for (final FieldNode field : fields) {
            count += field.children().isEmpty() ? 1 : countLeaves(field.children());
        }
        return count;
    }

    /**
     * Spec §9.3: a duplicate field name within one field list is a header
     * defect, checked recursively at every nesting level.
     */
    static void validateNoDuplicateFields(final List<FieldNode> fields, final DecodeContext context) {
        final Set<String> seen = new HashSet<>(fields.size());
        for (final FieldNode field : fields) {
            if (!seen.add(field.name())) {
                throw new IllegalArgumentException(
                    "Duplicate field name '" + field.name() + "' in tabular header");
            }
            if (!field.children().isEmpty()) {
                validateNoDuplicateFields(field.children(), context);
            }
        }
    }
}
