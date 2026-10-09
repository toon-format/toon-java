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
 * <p>A tabular row must contain exactly the same number of values as the header
 * declares leaf fields, in strict and non-strict mode alike (§14.4 recovery 1
 * keeps width checking); an {@link IllegalArgumentException} is thrown otherwise.</p>
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

        final int expectedRowDepth = DecodeHelper.findContentDepth(depth, context);

        while (context.currentLine < context.lines.length) {
            if (!processTabularArrayLine(depth, expectedRowDepth, fields, arrayDelimiter, result, context)) {
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
        validateKeysDelimiter(keysStr, arrayDelimiter);

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
        boolean grouped = false;
        int i = start;
        while (i < fieldList.length()) {
            final char c = fieldList.charAt(i);
            rejectContentAfterGroup(c, grouped, delimiterChar);
            if (escaped) {
                name.append(c);
                escaped = false;
                i++;
            } else if (inQuotes && c == BACKSLASH) {
                name.append(c);
                escaped = true;
                i++;
            } else if (c == DOUBLE_QUOTE) {
                name.append(c);
                inQuotes = !inQuotes;
                i++;
            } else if (!inQuotes && c == '{') {
                i = parseNestedFieldGroup(fieldList, i, arrayDelimiter, context, result, name);
                // The name buffer is consumed only when the group was added
                grouped = name.isEmpty();
            } else if (!inQuotes && c == '}') {
                flushField(result, name, grouped);
                return i + 1;
            } else if (!inQuotes && c == delimiterChar) {
                i = skipFieldDelimiter(fieldList, i, result, name, grouped);
                grouped = false;
            } else {
                name.append(c);
                i++;
            }
        }
        flushField(result, name, grouped);
        return -1;
    }

    /**
     * Rejects content directly after a completed nested field group unless
     * it is a space, the closing brace, or the delimiter (§6).
     *
     * @param c             the character following the group
     * @param grouped       whether a nested field group was just completed
     * @param delimiterChar the type of delimiter used in the array
     */
    private static void rejectContentAfterGroup(final char c, final boolean grouped, final char delimiterChar) {
        if (grouped && c != ' ' && c != '}' && c != delimiterChar) {
            throw new IllegalArgumentException("Unexpected content after nested field group");
        }
    }

    /**
     * Parses a nested field group opened at the given brace, recursing into
     * {@link #parseFieldList}. Unbalanced groups are rejected in strict mode;
     * in lenient mode their children are dropped and the name stays a leaf
     * field. A missing name or whitespace before the brace is rejected in any
     * mode.
     *
     * @param fieldList      the field list string to parse
     * @param braceIdx       the index of the opening brace
     * @param arrayDelimiter the type of delimiter used in the array
     * @param context        decode an object to deal with lines, delimiter and options
     * @param result         the list to add the parsed group field to
     * @param name           the buffered group field name
     * @return the index just past the closing brace, or the end of the string
     *         when the group is unbalanced and lenient mode skips it
     */
    private static int parseNestedFieldGroup(final String fieldList, final int braceIdx,
            final Delimiter arrayDelimiter, final DecodeContext context, final List<FieldNode> result,
            final StringBuilder name) {
        if (DecodeHelper.trimSpaces(name.toString()).isEmpty()) {
            throw new IllegalArgumentException("Missing field name before nested field group");
        }
        if (name.charAt(name.length() - 1) == ' ') {
            throw new IllegalArgumentException("Whitespace before nested field group");
        }
        final List<FieldNode> children = new ArrayList<>();
        final int next = parseFieldList(fieldList, braceIdx + 1, arrayDelimiter, context, children);
        if (next < 0) {
            if (context.options.strict()) {
                throw new IllegalArgumentException(
                    "Unbalanced braces in tabular header field list");
            }
            return fieldList.length();
        }
        result.add(new FieldNode(decodeFieldName(name), children));
        name.setLength(0);
        return next;
    }

    /**
     * Flushes the buffered field and skips the delimiter together with any
     * following whitespace.
     *
     * @param fieldList    the field list string to parse
     * @param delimiterIdx the index of the delimiter character
     * @param result       the list to add the flushed field to
     * @param name         the buffered field name
     * @param grouped      whether the entry already ended with a nested field group
     * @return the index just past the delimiter and trailing spaces
     */
    private static int skipFieldDelimiter(final String fieldList, final int delimiterIdx,
            final List<FieldNode> result, final StringBuilder name, final boolean grouped) {
        flushField(result, name, grouped);
        int i = delimiterIdx + 1;
        while (i < fieldList.length() && fieldList.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    /**
     * Adds the buffered field name as a leaf node and resets the buffer. An
     * empty entry is a header error unless a nested field group ended it.
     */
    private static void flushField(final List<FieldNode> result, final StringBuilder name, final boolean grouped) {
        if (!DecodeHelper.trimSpaces(name.toString()).isEmpty()) {
            result.add(new FieldNode(decodeFieldName(name), Collections.emptyList()));
        } else if (!grouped) {
            throw new IllegalArgumentException("Empty field entry in tabular header field list");
        }
        name.setLength(0);
    }

    /**
     * Decodes a buffered field name: trims spaces, enforces the quoted-token
     * boundary (§7.4), and unescapes it.
     */
    private static String decodeFieldName(final StringBuilder name) {
        final String rawName = DecodeHelper.trimSpaces(name.toString());
        DecodeHelper.validateQuotedTokenBoundary(rawName);
        return StringEscaper.unescape(rawName);
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
            } else if (inQuotes && c == BACKSLASH) {
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
        final boolean isDelimiterChar = actualChar == Delimiter.COMMA.getValue()
                || actualChar == Delimiter.TAB.getValue()
                || actualChar == Delimiter.PIPE.getValue();
        if (isDelimiterChar && actualChar != expectedChar) {
            throw new IllegalArgumentException(
                "Delimiter mismatch: bracket declares '" + expectedChar
                        + "', field list uses '" + actualChar + "'");
        }
    }

    /**
     * Processes a single line in a tabular array.
     *
     * @param headerDepth      the depth of the array header
     * @param expectedRowDepth the expected depth of the next row
     * @param fields           the field tree for the tabular array
     * @param arrayDelimiter   the type of delimiter used in the array
     * @param result           the list to store parsed rows in
     * @param context          decode an object to deal with lines, delimiter and options
     * @return true if parsing should continue, false if an array should terminate
     */
    private static boolean processTabularArrayLine(final int headerDepth, final int expectedRowDepth,
            final List<FieldNode> fields, final Delimiter arrayDelimiter, final List<Object> result,
            final DecodeContext context) {
        final String line = context.lines[context.currentLine];

        if (DecodeHelper.isBlankLine(line)) {
            // Spec §12: blank lines between the header and the first row are
            // accepted even in strict mode
            if (result.isEmpty()) {
                context.currentLine++;
                return true;
            }
            return !handleBlankLineInTabularArray(headerDepth, context);
        }

        final int lineDepth = DecodeHelper.getDepth(line, context);
        // A line between the header and an adopted deeper row depth belongs to no scope
        if (lineDepth > headerDepth && lineDepth < expectedRowDepth) {
            throw DecodeHelper.overIndentedLineError(context, lineDepth);
        }
        if (shouldTerminateTabularArray(line, lineDepth, expectedRowDepth, arrayDelimiter, context)) {
            return false;
        }

        processTabularRow(line, lineDepth, expectedRowDepth, fields, arrayDelimiter, result, context);
        context.currentLine++;
        return true;
    }

    /**
     * Handles blank line processing in a tabular array.
     *
     * @param headerDepth the depth of the array header
     * @param context     decode an object to deal with lines, delimiter and options
     * @return true if an array should terminate, false if a line should be skipped
     */
    private static boolean handleBlankLineInTabularArray(final int headerDepth, final DecodeContext context) {
        final int nextNonBlankLine = DecodeHelper.findNextNonBlankLine(context.currentLine + 1, context);

        if (nextNonBlankLine >= context.lines.length) {
            // Blank lines at the end of the document are trailing newlines (§12)
            return true;
        }
        final int nextDepth = DecodeHelper.getDepth(context.lines[nextNonBlankLine], context);
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
     * @param arrayDelimiter   the active delimiter declared by the header
     * @param context          decode an object to deal with lines, delimiter and options
     * @return true if an array should terminate, false otherwise.
     */
    private static boolean shouldTerminateTabularArray(final String line, final int lineDepth,
            final int expectedRowDepth, final Delimiter arrayDelimiter, final DecodeContext context) {
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
        final char delimChar = arrayDelimiter.getValue();
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
            } else if (inQuotes && c == '\\') {
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
     * Processes a tabular row at the expected depth.
     *
     * @param line             the line to process
     * @param lineDepth        the depth of the line, at least the expected row depth
     * @param expectedRowDepth the expected depth of the next row
     * @param fields           the field tree for the tabular array
     * @param arrayDelimiter   the type of delimiter used in the array
     * @param result           the list to store parsed rows in
     * @param context          decode an object to deal with lines, delimiter and options
     */
    private static void processTabularRow(final String line, final int lineDepth,
            final int expectedRowDepth, final List<FieldNode> fields, final Delimiter arrayDelimiter,
            final List<Object> result, final DecodeContext context) {
        if (lineDepth > expectedRowDepth) {
            // A line deeper than the row depth belongs to no scope (§14.2)
            throw DecodeHelper.overIndentedLineError(context, lineDepth);
        }
        final String rowContent = line.substring(expectedRowDepth * context.options.indent());
        result.add(parseTabularRow(rowContent, fields, arrayDelimiter, context));
    }

    /**
     * Parses a tabular row into a Map using the provided field tree.
     * A leaf field consumes the next cell; a nested field group materializes
     * an object from its subfields (§9.3).
     *
     * <p>The number of values must exactly match the leaf-field count in strict
     * and non-strict mode alike (§14.4 recovery 1 keeps width checking); an
     * {@link IllegalArgumentException} is thrown otherwise.</p>
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
        if (values.size() != countLeaves(fields)) {
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
