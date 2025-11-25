package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.PathExpansion;
import dev.toonformat.jtoon.util.StringEscaper;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main decoder for converting TOON-formatted strings to Java objects.
 *
 * <p>
 * Implements a line-by-line parser with indentation-based depth tracking.
 * Delegates primitive type inference to {@link PrimitiveDecoder}.
 * </p>
 *
 * <h2>Parsing Strategy:</h2>
 * <ul>
 * <li>Split input into lines</li>
 * <li>Track current line position and indentation depth</li>
 * <li>Use regex patterns to detect structure (arrays, objects, primitives)</li>
 * <li>Recursively process nested structures</li>
 * </ul>
 *
 * @see DecodeOptions
 * @see PrimitiveDecoder
 */
public final class ValueDecoder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Matches standalone array headers: [3], [#2], [3\t], [2|]
     * Group 1: optional # marker, Group 2: digits, Group 3: optional delimiter
     */
    private static final Pattern ARRAY_HEADER_PATTERN = Pattern.compile("^\\[(#?)(\\d+)([\\t|])?]");

    /**
     * Matches tabular array headers with field names: [2]{id,name,role}:
     * Group 1: optional # marker, Group 2: digits, Group 3: optional delimiter,
     * Group 4: field spec
     */
    private static final Pattern TABULAR_HEADER_PATTERN = Pattern.compile("^\\[(#?)(\\d+)([\\t|])?]\\{(.+)}:");

    /**
     * Matches keyed array headers: items[2]{id,name}: or tags[3]: or data[4]{id}:
     * Captures: group(1)=key, group(2)=#marker, group(3)=delimiter,
     * group(4)=optional field spec
     */
    private static final Pattern KEYED_ARRAY_PATTERN = Pattern.compile("^(.+?)\\[(#?)\\d+([\\t|])?](\\{[^}]+})?:.*$");

    private ValueDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Decodes a TOON-formatted string to a Java object.
     *
     * @param toon    TOON-formatted input string
     * @param options parsing options (delimiter, indentation, strict mode)
     * @return parsed object (Map, List, primitive, or null)
     * @throws IllegalArgumentException if strict mode is enabled and input is
     *                                  invalid
     */
    public static Object decode(String toon, DecodeOptions options) {
        if (toon == null || toon.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }

        // Special case: if input is exactly "null", return null
        String trimmed = toon.trim();
        if ("null".equals(trimmed)) {
            return null;
        }

        // Don't trim leading whitespace - we need it for indentation validation
        // Only trim trailing whitespace to avoid issues with empty lines at the end
        String processed = toon;
        while (!processed.isEmpty() && Character.isWhitespace(processed.charAt(processed.length() - 1))) {
            processed = processed.substring(0, processed.length() - 1);
        }

        Parser parser = new Parser(processed, options);
        Object result = parser.parseValue();
        // If result is null (no content), return empty object
        if (result == null) {
            return new LinkedHashMap<>();
        }
        return result;
    }

    /**
     * Decodes a TOON-formatted string directly to a JSON string using custom
     * options.
     *
     * <p>
     * This is a convenience method that decodes TOON to Java objects and then
     * serializes them to JSON.
     * </p>
     *
     * @param toon    The TOON-formatted string to decode
     * @param options Decoding options (indent, delimiter, strict mode)
     * @return JSON string representation
     * @throws IllegalArgumentException if strict mode is enabled and input is
     *                                  invalid
     */
    public static String decodeToJson(String toon, DecodeOptions options) {
        try {
            Object decoded = ValueDecoder.decode(toon, options);
            return OBJECT_MAPPER.writeValueAsString(decoded);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert decoded value to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Inner parser class managing line-by-line parsing state.
     * Maintains currentLine index and uses recursive descent for nested structures.
     */
    private static class Parser {
        private final String[] lines;
        private final DecodeOptions options;
        private final String delimiter;
        private int currentLine = 0;

        Parser(String toon, DecodeOptions options) {
            this.lines = toon.split("\n", -1);
            this.options = options;
            this.delimiter = options.delimiter().getValue();
        }

        /**
         * Parses the current line at root level (depth 0).
         * Routes to appropriate handler based online content.
         */
        Object parseValue() {
            if (currentLine >= lines.length) {
                return null;
            }

            String line = lines[currentLine];
            int depth = getDepth(line);

            if (depth > 0) {
                return handleUnexpectedIndentation();
            }

            String content = line.substring(depth * options.indent());

            // Handle standalone arrays: [2]:
            if (content.startsWith("[")) {
                return parseArray(content, depth);
            }

            // Handle keyed arrays: items[2]{id,name}:
            Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
            if (keyedArray.matches()) {
                return parseKeyedArrayValue(keyedArray, content, depth);
            }

            // Handle key-value pairs: name: Ada
            int colonIdx = findUnquotedColon(content);
            if (colonIdx > 0) {
                String key = content.substring(0, colonIdx).trim();
                String value = content.substring(colonIdx + 1).trim();
                return parseKeyValuePair(key, value, depth, depth == 0);
            }

            // Bare scalar value
            return parseBareScalarValue(content, depth);
        }

        /**
         * Handles unexpected indentation at root level.
         */
        private Object handleUnexpectedIndentation() {
            if (options.strict()) {
                throw new IllegalArgumentException("Unexpected indentation at line " + currentLine);
            }
            return null;
        }

        /**
         * Parses a keyed array value (e.g., "items[2]{id,name}:").
         */
        private Object parseKeyedArrayValue(Matcher keyedArray, String content, int depth) {
            String originalKey = keyedArray.group(1).trim();
            String key = StringEscaper.unescape(originalKey);
            String arrayHeader = content.substring(keyedArray.group(1).length());

            var arrayValue = parseArray(arrayHeader, depth);
            Map<String, Object> obj = new LinkedHashMap<>();

            // Handle path expansion for array keys
            if (shouldExpandKey(originalKey)) {
                expandPathIntoMap(obj, key, arrayValue);
            } else {
                // Check for conflicts with existing expanded paths
                checkPathExpansionConflict(obj, key, arrayValue);
                obj.put(key, arrayValue);
            }

            // Continue parsing root-level fields if at depth 0
            if (depth == 0) {
                parseRootObjectFields(obj, depth);
            }

            return obj;
        }

        /**
         * Parses a bare scalar value and validates in strict mode.
         */
        private Object parseBareScalarValue(String content, int depth) {
            Object result = PrimitiveDecoder.parse(content);
            currentLine++;

            // In strict mode, check if there are more primitives at root level
            if (options.strict() && depth == 0) {
                validateNoMultiplePrimitivesAtRoot();
            }

            return result;
        }

        /**
         * Validates that there are no multiple primitives at root level in strict mode.
         */
        private void validateNoMultiplePrimitivesAtRoot() {
            int lineIndex = currentLine;
            while (lineIndex < lines.length && isBlankLine(lines[lineIndex])) {
                lineIndex++;
            }
            if (lineIndex < lines.length) {
                int nextDepth = getDepth(lines[lineIndex]);
                if (nextDepth == 0) {
                    throw new IllegalArgumentException(
                            "Multiple primitives at root depth in strict mode at line " + (lineIndex + 1));
                }
            }
        }

        /**
         * Extracts delimiter from array header.
         * Returns tab, pipe, or comma (default) based on header pattern.
         */
        private String extractDelimiterFromHeader(String header) {
            Matcher matcher = ARRAY_HEADER_PATTERN.matcher(header);
            if (matcher.find()) {
                String delimChar = matcher.group(3);
                if (delimChar != null) {
                    if ("\t".equals(delimChar)) {
                        return "\t";
                    } else if ("|".equals(delimChar)) {
                        return "|";
                    }
                }
            }
            // Default to comma
            return delimiter;
        }

        /**
         * Extracts declared length from array header.
         * Returns the number specified in [n] or null if not found.
         */
        private Integer extractLengthFromHeader(String header) {
            Matcher matcher = ARRAY_HEADER_PATTERN.matcher(header);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(2));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        /**
         * Validates array length if declared in header.
         */
        private void validateArrayLength(String header, int actualLength) {
            Integer declaredLength = extractLengthFromHeader(header);
            if (declaredLength != null && declaredLength != actualLength) {
                throw new IllegalArgumentException(
                        String.format("Array length mismatch: declared %d, found %d", declaredLength, actualLength));
            }
        }

        /**
         * Parses array from header string and following lines with a specific
         * delimiter.
         * Detects array type (tabular, list, or primitive) and routes accordingly.
         */
        private List<Object> parseArrayWithDelimiter(String header, int depth, String arrayDelimiter) {
            Matcher tabularMatcher = TABULAR_HEADER_PATTERN.matcher(header);
            Matcher arrayMatcher = ARRAY_HEADER_PATTERN.matcher(header);

            if (tabularMatcher.find()) {
                return parseTabularArray(header, depth, arrayDelimiter);
            }

            if (arrayMatcher.find()) {
                int headerEndIdx = arrayMatcher.end();
                String afterHeader = header.substring(headerEndIdx).trim();

                if (afterHeader.startsWith(":")) {
                    String inlineContent = afterHeader.substring(1).trim();

                    if (!inlineContent.isEmpty()) {
                        List<Object> result = parseArrayValues(inlineContent, arrayDelimiter);
                        validateArrayLength(header, result.size());
                        currentLine++;
                        return result;
                    }
                }

                currentLine++;
                if (currentLine < lines.length) {
                    String nextLine = lines[currentLine];
                    int nextDepth = getDepth(nextLine);
                    String nextContent = nextLine.substring(nextDepth * options.indent());

                    if (nextDepth <= depth) {
                        // The next line is not a child of this array,
                        // the array is empty
                        List<Object> empty = new ArrayList<>();
                        validateArrayLength(header, 0);
                        return empty;
                    }

                    if (nextContent.startsWith("- ")) {
                        currentLine--;
                        return parseListArray(depth, header);
                    } else {
                        currentLine++;
                        List<Object> result = parseArrayValues(nextContent, arrayDelimiter);
                        validateArrayLength(header, result.size());
                        return result;
                    }
                }
                List<Object> empty = new ArrayList<>();
                validateArrayLength(header, 0);
                return empty;
            }

            if (options.strict()) {
                throw new IllegalArgumentException("Invalid array header: " + header);
            }
            return Collections.emptyList();
        }

        /**
         * Parses array from header string and following lines.
         * Detects array type (tabular, list, or primitive) and routes accordingly.
         */
        private List<Object> parseArray(String header, int depth) {
            String arrayDelimiter = extractDelimiterFromHeader(header);

            return parseArrayWithDelimiter(header, depth, arrayDelimiter);
        }

        /**
         * Checks if a line is blank (empty or only whitespace).
         */
        private boolean isBlankLine(String line) {
            return line.trim().isEmpty();
        }

        /**
         * Parses tabular array format where each row contains delimiter-separated
         * values.
         * Example: items[2]{id,name}:\n 1,Ada\n 2,Bob
         */
        private List<Object> parseTabularArray(String header, int depth, String arrayDelimiter) {
            Matcher matcher = TABULAR_HEADER_PATTERN.matcher(header);
            if (!matcher.find()) {
                return new ArrayList<>();
            }

            String keysStr = matcher.group(4);
            List<String> keys = parseTabularKeys(keysStr, arrayDelimiter);

            List<Object> result = new ArrayList<>();
            currentLine++;

            // Determine the expected row depth dynamically from the first non-blank line
            int expectedRowDepth;
            if (currentLine < lines.length) {
                int nextNonBlankLine = findNextNonBlankLine(currentLine);
                if (nextNonBlankLine < lines.length) {
                    expectedRowDepth = getDepth(lines[nextNonBlankLine]);
                } else {
                    expectedRowDepth = depth + 1;
                }
            } else {
                expectedRowDepth = depth + 1;
            }

            while (currentLine < lines.length) {
                if (!processTabularArrayLine(expectedRowDepth, keys, arrayDelimiter, result)) {
                    break;
                }
            }

            validateArrayLength(header, result.size());
            return result;
        }

        /**
         * Processes a single line in a tabular array.
         * Returns true if parsing should continue, false if an array should terminate.
         */
        private boolean processTabularArrayLine(int expectedRowDepth, List<String> keys, String arrayDelimiter,
                                                List<Object> result) {
            String line = lines[currentLine];

            if (isBlankLine(line)) {
                return !handleBlankLineInTabularArray(expectedRowDepth);
            }

            int lineDepth = getDepth(line);
            if (shouldTerminateTabularArray(line, lineDepth, expectedRowDepth)) {
                return false;
            }

            if (processTabularRow(line, lineDepth, expectedRowDepth, keys, arrayDelimiter, result)) {
                currentLine++;
            }
            return true;
        }

        /**
         * Handles blank line processing in a tabular array.
         * Returns true if an array should terminate, false if a line should be skipped.
         */
        private boolean handleBlankLineInTabularArray(int expectedRowDepth) {
            int nextNonBlankLine = findNextNonBlankLine(currentLine + 1);

            if (nextNonBlankLine < lines.length) {
                int nextDepth = getDepth(lines[nextNonBlankLine]);
                // Header depth is one level above the expected row depth
                int headerDepth = expectedRowDepth - 1;
                if (nextDepth <= headerDepth) {
                    return true;
                }
            }

            // Blank line is inside the array
            if (options.strict()) {
                throw new IllegalArgumentException(
                        "Blank line inside tabular array at line " + (currentLine + 1));
            }
            // In non-strict mode, skip blank lines
            currentLine++;
            return false;
        }

        /**
         * Finds the next non-blank line starting from the given index.
         */
        private int findNextNonBlankLine(int startIndex) {
            int index = startIndex;
            while (index < lines.length && isBlankLine(lines[index])) {
                index++;
            }
            return index;
        }

        /**
         * Determines if tabular array parsing should terminate based online depth.
         * Returns true if array should terminate, false otherwise.
         */
        private boolean shouldTerminateTabularArray(String line, int lineDepth, int expectedRowDepth) {
            // Header depth is one level above expected row depth
            int headerDepth = expectedRowDepth - 1;

            if (lineDepth < expectedRowDepth) {
                if (lineDepth == headerDepth) {
                    String content = line.substring(headerDepth * options.indent());
                    int colonIdx = findUnquotedColon(content);
                    if (colonIdx > 0) {
                        return true; // Key-value pair at same depth - terminate array
                    }
                }
                return true; // Line depth is less than expected - terminate
            }

            // Check for key-value pair at expected row depth
            if (lineDepth == expectedRowDepth) {
                String rowContent = line.substring(expectedRowDepth * options.indent());
                int colonIdx = findUnquotedColon(rowContent);
                return colonIdx > 0; // Key-value pair at same depth as rows - terminate array
            }

            return false;
        }

        /**
         * Processes a tabular row if it matches the expected depth.
         * Returns true if line was processed and currentLine should be incremented,
         * false otherwise.
         */
        private boolean processTabularRow(String line, int lineDepth, int expectedRowDepth, List<String> keys,
                                          String arrayDelimiter, List<Object> result) {
            if (lineDepth == expectedRowDepth) {
                String rowContent = line.substring(expectedRowDepth * options.indent());
                Map<String, Object> row = parseTabularRow(rowContent, keys, arrayDelimiter);
                result.add(row);
                return true;
            } else if (lineDepth > expectedRowDepth) {
                // Line is deeper than expected - might be nested content, skip it
                currentLine++;
                return false;
            }
            return true;
        }

        /**
         * Parses list array format where items are prefixed with "- ".
         * Example: items[2]:\n - item1\n - item2
         */
        private List<Object> parseListArray(int depth, String header) {
            List<Object> result = new ArrayList<>();
            currentLine++;

            boolean shouldContinue = true;
            while (shouldContinue && currentLine < lines.length) {
                String line = lines[currentLine];

                if (isBlankLine(line)) {
                    if (handleBlankLineInListArray(depth)) {
                        shouldContinue = false;
                    }
                } else {
                    int lineDepth = getDepth(line);
                    if (shouldTerminateListArray(lineDepth, depth, line)) {
                        shouldContinue = false;
                    } else {
                        processListArrayItem(line, lineDepth, depth, result);
                    }
                }
            }

            if (header != null) {
                validateArrayLength(header, result.size());
            }
            return result;
        }

        /**
         * Handles blank line processing in list array.
         * Returns true if array should terminate, false if line should be skipped.
         */
        private boolean handleBlankLineInListArray(int depth) {
            int nextNonBlankLine = findNextNonBlankLine(currentLine + 1);

            if (nextNonBlankLine >= lines.length) {
                return true; // End of file - terminate array
            }

            int nextDepth = getDepth(lines[nextNonBlankLine]);
            if (nextDepth <= depth) {
                return true; // Blank line is outside array - terminate
            }

            // Blank line is inside array
            if (options.strict()) {
                throw new IllegalArgumentException("Blank line inside list array at line " + (currentLine + 1));
            }
            // In non-strict mode, skip blank lines
            currentLine++;
            return false;
        }

        /**
         * Determines if list array parsing should terminate based online depth.
         * Returns true if array should terminate, false otherwise.
         */
        private boolean shouldTerminateListArray(int lineDepth, int depth, String line) {
            if (lineDepth < depth + 1) {
                return true; // Line depth is less than expected - terminate
            }
            // Also terminate if line is at expected depth but doesn't start with "-"
            if (lineDepth == depth + 1) {
                String content = line.substring((depth + 1) * options.indent());
                return !content.startsWith("-"); // Not an array item - terminate
            }
            return false;
        }

        /**
         * Processes a single list array item if it matches the expected depth.
         */
        private void processListArrayItem(String line, int lineDepth, int depth, List<Object> result) {
            if (lineDepth == depth + 1) {
                String content = line.substring((depth + 1) * options.indent());

                if (content.startsWith("-")) {
                    result.add(parseListItem(content, depth));
                } else {
                    currentLine++;
                }
            } else {
                currentLine++;
            }
        }

        /**
         * Parses a single list item starting with "- ".
         * Item can be a scalar value or an object with nested fields.
         */
        private Object parseListItem(String content, int depth) {
            // Handle empty item: just "-" or "- "
            String itemContent;
            if (content.length() > 2) {
                itemContent = content.substring(2).trim();
            } else {
                itemContent = "";
            }

            // Handle empty item: just "-"
            if (itemContent.isEmpty()) {
                currentLine++;
                return new LinkedHashMap<>();
            }

            // Check for standalone array (e.g., "[2]: 1,2")
            if (itemContent.startsWith("[")) {
                // For nested arrays in list items, default to comma delimiter if not specified
                String nestedArrayDelimiter = extractDelimiterFromHeader(itemContent);
                // parseArrayWithDelimiter handles currentLine increment internally
                // For inline arrays, it increments. For multi-line arrays, parseListArray
                // handles it.
                // We need to increment here only if it was an inline array that we just parsed
                // Actually, parseArrayWithDelimiter always handles currentLine, so we don't
                // need to increment
                return parseArrayWithDelimiter(itemContent, depth + 1, nestedArrayDelimiter);
            }

            // Check for keyed array pattern (e.g., "tags[3]: a,b,c" or "data[2]{id}: ...")
            Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(itemContent);
            if (keyedArray.matches()) {
                String originalKey = keyedArray.group(1).trim();
                String key = StringEscaper.unescape(originalKey);
                String arrayHeader = itemContent.substring(keyedArray.group(1).length());

                // For nested arrays in list items, default to comma delimiter if not specified
                String nestedArrayDelimiter = extractDelimiterFromHeader(arrayHeader);
                List<Object> arrayValue = parseArrayWithDelimiter(arrayHeader, depth + 2, nestedArrayDelimiter);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put(key, arrayValue);

                // parseArrayWithDelimiter manages currentLine correctly:
                // - For inline arrays, it increments currentLine
                // - For multi-line arrays (list/tabular), the array parsers leave currentLine
                // at the line after the array
                // So we don't need to increment here. Just parse additional fields.
                parseListItemFields(item, depth);

                return item;
            }

            int colonIdx = findUnquotedColon(itemContent);

            // Simple scalar: - value
            if (colonIdx <= 0) {
                currentLine++;
                return PrimitiveDecoder.parse(itemContent);
            }

            // Object item: - key: value
            String key = StringEscaper.unescape(itemContent.substring(0, colonIdx).trim());
            String value = itemContent.substring(colonIdx + 1).trim();

            currentLine++;

            Map<String, Object> item = new LinkedHashMap<>();
            Object parsedValue;
            // If no next line exists, handle simple case
            if (currentLine >= lines.length) {
                parsedValue = value.trim().isEmpty() ? new LinkedHashMap<>() : PrimitiveDecoder.parse(value);
            } else {
                // List item is at depth + 1, so pass depth + 1 to parseObjectItemValue
                parsedValue = parseObjectItemValue(value, depth + 1);
            }
            item.put(key, parsedValue);
            parseListItemFields(item, depth);

            return item;
        }

        /**
         * Parses the value portion of an object item in a list, handling nested
         * objects,
         * empty values, and primitives.
         *
         * @param value the value string to parse
         * @param depth the depth of the list item
         * @return the parsed value (Map, List, or primitive)
         */
        private Object parseObjectItemValue(String value, int depth) {
            boolean isEmpty = value.trim().isEmpty();

            // Find next non-blank line and its depth
            Integer nextDepth = findNextNonBlankLineDepth();
            if (nextDepth == null) {
                // No non-blank line found - create empty object
                return new LinkedHashMap<>();
            }

            // Handle empty value with nested content
            // The list item is at depth, and the field itself is conceptually at depth + 1
            // So nested content should be parsed with parentDepth = depth + 1
            // This allows nested fields at depth + 2 or deeper to be processed correctly
            if (isEmpty && nextDepth > depth) {
                return parseNestedObject(depth + 1);
            }

            // Handle empty value without nested content or non-empty value
            return isEmpty ? new LinkedHashMap<>() : PrimitiveDecoder.parse(value);
        }

        /**
         * Finds the depth of the next non-blank line, skipping blank lines.
         *
         * @return the depth of the next non-blank line, or null if none exists
         */
        private Integer findNextNonBlankLineDepth() {
            int nextLineIdx = currentLine;
            while (nextLineIdx < lines.length && isBlankLine(lines[nextLineIdx])) {
                nextLineIdx++;
            }

            if (nextLineIdx >= lines.length) {
                return null;
            }

            return getDepth(lines[nextLineIdx]);
        }

        /**
         * Parses a field value, handling nested objects, empty values, and primitives.
         *
         * @param fieldValue the value string to parse
         * @param fieldDepth the depth at which the field is located
         * @return the parsed value (Map, List, or primitive)
         */
        private Object parseFieldValue(String fieldValue, int fieldDepth) {
            // Check if next line is nested
            if (currentLine + 1 < lines.length) {
                int nextDepth = getDepth(lines[currentLine + 1]);
                if (nextDepth > fieldDepth) {
                    currentLine++;
                    // parseNestedObject manages currentLine, so we don't increment here
                    return parseNestedObject(fieldDepth);
                } else {
                    // If value is empty, create empty object; otherwise parse as primitive
                    if (fieldValue.trim().isEmpty()) {
                        currentLine++;
                        return new LinkedHashMap<>();
                    } else {
                        currentLine++;
                        return PrimitiveDecoder.parse(fieldValue);
                    }
                }
            } else {
                // If value is empty, create empty object; otherwise parse as primitive
                if (fieldValue.trim().isEmpty()) {
                    currentLine++;
                    return new LinkedHashMap<>();
                } else {
                    currentLine++;
                    return PrimitiveDecoder.parse(fieldValue);
                }
            }
        }

        /**
         * Parses a keyed array field and adds it to the item map.
         *
         * @param fieldContent the field content to parse
         * @param item         the map to add the field to
         * @param depth        the depth of the list item
         * @return true if the field was processed as a keyed array, false otherwise
         */
        private boolean parseKeyedArrayField(String fieldContent, Map<String, Object> item, int depth) {
            Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(fieldContent);
            if (!keyedArray.matches()) {
                return false;
            }

            String originalKey = keyedArray.group(1).trim();
            String key = StringEscaper.unescape(originalKey);
            String arrayHeader = fieldContent.substring(keyedArray.group(1).length());

            // For nested arrays in list items, default to comma delimiter if not specified
            String nestedArrayDelimiter = extractDelimiterFromHeader(arrayHeader);
            var arrayValue = parseArrayWithDelimiter(arrayHeader, depth + 2, nestedArrayDelimiter);

            // Handle path expansion for array keys
            if (shouldExpandKey(originalKey)) {
                expandPathIntoMap(item, key, arrayValue);
            } else {
                item.put(key, arrayValue);
            }

            // parseArrayWithDelimiter manages currentLine correctly
            return true;
        }

        /**
         * Parses a key-value field and adds it to the item map.
         *
         * @param fieldContent the field content to parse
         * @param item         the map to add the field to
         * @param depth        the depth of the list item
         * @return true if the field was processed as a key-value pair, false otherwise
         */
        private boolean parseKeyValueField(String fieldContent, Map<String, Object> item, int depth) {
            int colonIdx = findUnquotedColon(fieldContent);
            if (colonIdx <= 0) {
                return false;
            }

            String fieldKey = StringEscaper.unescape(fieldContent.substring(0, colonIdx).trim());
            String fieldValue = fieldContent.substring(colonIdx + 1).trim();

            Object parsedValue = parseFieldValue(fieldValue, depth + 2);

            // Handle path expansion
            if (shouldExpandKey(fieldKey)) {
                expandPathIntoMap(item, fieldKey, parsedValue);
            } else {
                item.put(fieldKey, parsedValue);
            }

            // parseFieldValue manages currentLine appropriately
            return true;
        }

        /**
         * Parses additional fields for a list item object.
         */
        private void parseListItemFields(Map<String, Object> item, int depth) {
            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int lineDepth = getDepth(line);

                if (lineDepth < depth + 2) {
                    return;
                }

                if (lineDepth == depth + 2) {
                    String fieldContent = line.substring((depth + 2) * options.indent());

                    // Try to parse as keyed array first, then as key-value pair
                    boolean wasParsed = parseKeyedArrayField(fieldContent, item, depth);
                    if (!wasParsed) {
                        wasParsed = parseKeyValueField(fieldContent, item, depth);
                    }

                    // If neither pattern matched, skip this line to avoid infinite loop
                    if (!wasParsed) {
                        currentLine++;
                    }
                } else {
                    // lineDepth > depth + 2, skip this line
                    currentLine++;
                }
            }
        }

        /**
         * Parses a tabular row into a Map using the provided keys.
         * Validates that the row uses the correct delimiter.
         */
        private Map<String, Object> parseTabularRow(String rowContent, List<String> keys, String arrayDelimiter) {
            Map<String, Object> row = new LinkedHashMap<>();
            List<Object> values = parseArrayValues(rowContent, arrayDelimiter);

            // Validate value count matches key count
            if (options.strict() && values.size() != keys.size()) {
                throw new IllegalArgumentException(
                        String.format("Tabular row value count (%d) does not match header field count (%d)",
                                values.size(), keys.size()));
            }

            for (int i = 0; i < keys.size() && i < values.size(); i++) {
                row.put(keys.get(i), values.get(i));
            }

            return row;
        }

        /**
         * Parses tabular header keys from field specification.
         * Validates delimiter consistency between bracket and brace fields.
         */
        private List<String> parseTabularKeys(String keysStr, String arrayDelimiter) {
            // Validate delimiter mismatch between bracket and brace fields
            if (options.strict()) {
                validateKeysDelimiter(keysStr, arrayDelimiter);
            }

            List<String> result = new ArrayList<>();
            List<String> rawValues = parseDelimitedValues(keysStr, arrayDelimiter);
            for (String key : rawValues) {
                result.add(StringEscaper.unescape(key));
            }
            return result;
        }

        /**
         * Validates delimiter consistency in tabular header keys.
         */
        private void validateKeysDelimiter(String keysStr, String expectedDelimiter) {
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
        private void checkDelimiterMismatch(char expectedChar, char actualChar) {
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
         * Parses array values from a delimiter-separated string.
         */
        private List<Object> parseArrayValues(String values, String arrayDelimiter) {
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
         */
        private List<String> parseDelimitedValues(String input, String arrayDelimiter) {
            List<String> result = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            boolean escaped = false;
            char delimChar = arrayDelimiter.charAt(0);

            int i = 0;
            while (i < input.length()) {
                char c = input.charAt(i);

                if (escaped) {
                    current.append(c);
                    escaped = false;
                    i++;
                } else if (c == '\\') {
                    current.append(c);
                    escaped = true;
                    i++;
                } else if (c == '"') {
                    current.append(c);
                    inQuotes = !inQuotes;
                    i++;
                } else if (c == delimChar && !inQuotes) {
                    // Found delimiter - add current value (trimmed) and reset
                    String value = current.toString().trim();
                    result.add(value);
                    current = new StringBuilder();
                    // Skip whitespace after delimiter
                    do {
                        i++;
                    } while (i < input.length() && Character.isWhitespace(input.charAt(i)));
                } else {
                    current.append(c);
                    i++;
                }
            }

            // Add final value
            if (!current.isEmpty() || input.endsWith(arrayDelimiter)) {
                result.add(current.toString().trim());
            }

            return result;
        }

        /**
         * Parses additional key-value pairs at root level.
         */
        private void parseRootObjectFields(Map<String, Object> obj, int depth) {
            while (currentLine < lines.length) {
                String line = lines[currentLine];
                int lineDepth = getDepth(line);

                if (lineDepth != depth) {
                    return;
                }

                // Skip blank lines
                if (isBlankLine(line)) {
                    currentLine++;
                    continue;
                }

                String content = line.substring(depth * options.indent());

                Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
                if (keyedArray.matches()) {
                    processRootKeyedArrayLine(obj, content, keyedArray, depth);
                } else {
                    int colonIdx = findUnquotedColon(content);
                    if (colonIdx > 0) {
                        String key = content.substring(0, colonIdx).trim();
                        String value = content.substring(colonIdx + 1).trim();

                        parseKeyValuePairIntoMap(obj, key, value, depth);
                    } else {
                        return;
                    }
                }
            }
        }

        /**
         * Processes a keyed array line in root object fields.
         */
        private void processRootKeyedArrayLine(Map<String, Object> obj, String content, Matcher keyedArray, int depth) {
            String originalKey = keyedArray.group(1).trim();
            String key = StringEscaper.unescape(originalKey);
            String arrayHeader = content.substring(keyedArray.group(1).length());

            var arrayValue = parseArray(arrayHeader, depth);

            // Handle path expansion for array keys
            if (shouldExpandKey(originalKey)) {
                expandPathIntoMap(obj, key, arrayValue);
            } else {
                // Check for conflicts with existing expanded paths
                checkPathExpansionConflict(obj, key, arrayValue);
                obj.put(key, arrayValue);
            }
        }

        /**
         * Parses nested object starting at currentLine.
         */
        private Map<String, Object> parseNestedObject(int parentDepth) {
            Map<String, Object> result = new LinkedHashMap<>();

            while (currentLine < lines.length) {
                String line = lines[currentLine];

                // Skip blank lines
                if (isBlankLine(line)) {
                    currentLine++;
                    continue;
                }

                int depth = getDepth(line);

                if (depth <= parentDepth) {
                    return result;
                }

                if (depth == parentDepth + 1) {
                    processDirectChildLine(result, line, parentDepth, depth);
                } else {
                    currentLine++;
                }
            }

            return result;
        }

        /**
         * Processes a line at depth == parentDepth + 1 (direct child).
         * Returns true if the line was processed, false if it was a blank line that was
         * skipped.
         */
        private void processDirectChildLine(Map<String, Object> result, String line, int parentDepth, int depth) {
            // Skip blank lines
            if (isBlankLine(line)) {
                currentLine++;
                return;
            }

            String content = line.substring((parentDepth + 1) * options.indent());
            Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);

            if (keyedArray.matches()) {
                processKeyedArrayLine(result, content, keyedArray, parentDepth);
            } else {
                processKeyValueLine(result, content, depth);
            }
        }

        /**
         * Processes a keyed array line (e.g., "key[3]: value").
         */
        private void processKeyedArrayLine(Map<String, Object> result, String content, Matcher keyedArray,
                                           int parentDepth) {
            String originalKey = keyedArray.group(1).trim();
            String key = StringEscaper.unescape(originalKey);
            String arrayHeader = content.substring(keyedArray.group(1).length());
            List<Object> arrayValue = parseArray(arrayHeader, parentDepth + 1);

            // Handle path expansion for array keys
            if (shouldExpandKey(originalKey)) {
                expandPathIntoMap(result, key, arrayValue);
            } else {
                // Check for conflicts with existing expanded paths
                checkPathExpansionConflict(result, key, arrayValue);
                result.put(key, arrayValue);
            }
        }

        /**
         * Processes a key-value line (e.g., "key: value").
         */
        private void processKeyValueLine(Map<String, Object> result, String content, int depth) {
            int colonIdx = findUnquotedColon(content);

            if (colonIdx > 0) {
                String key = content.substring(0, colonIdx).trim();
                String value = content.substring(colonIdx + 1).trim();
                parseKeyValuePairIntoMap(result, key, value, depth);
            } else {
                // No colon found in key-value context - this is an error
                if (options.strict()) {
                    throw new IllegalArgumentException(
                            "Missing colon in key-value context at line " + (currentLine + 1));
                }
                currentLine++;
            }
        }

        /**
         * Checks if a key should be expanded (is a valid identifier segment).
         * Keys with dots that are valid identifiers can be expanded.
         * Quoted keys are never expanded.
         */
        private boolean shouldExpandKey(String key) {
            if (options.expandPaths() != PathExpansion.SAFE) {
                return false;
            }
            // Quoted keys should not be expanded
            if (key.trim().startsWith("\"") && key.trim().endsWith("\"")) {
                return false;
            }
            // Check if key contains dots and is a valid identifier pattern
            if (!key.contains(".")) {
                return false;
            }
            // Valid identifier: starts with letter or underscore, followed by letters,
            // digits, underscores
            // Each segment must match this pattern
            String[] segments = key.split("\\.");
            for (String segment : segments) {
                if (!segment.matches("^[a-zA-Z_]\\w*$")) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Expands a dotted key into nested object structure.
         */
        private void expandPathIntoMap(Map<String, Object> map, String dottedKey, Object value) {
            String[] segments = dottedKey.split("\\.");
            Map<String, Object> current = map;

            // Navigate/create nested structure
            for (int i = 0; i < segments.length - 1; i++) {
                String segment = segments[i];
                Object existing = current.get(segment);

                if (existing == null) {
                    // Create new nested object
                    Map<String, Object> nested = new LinkedHashMap<>();
                    current.put(segment, nested);
                    current = nested;
                } else if (existing instanceof Map) {
                    // Use existing nested object
                    @SuppressWarnings("unchecked")
                    Map<String, Object> existingMap = (Map<String, Object>) existing;
                    current = existingMap;
                } else {
                    // Conflict: existing is not a Map
                    if (options.strict()) {
                        throw new IllegalArgumentException(
                                String.format("Path expansion conflict: %s is %s, cannot expand to object",
                                        segment, existing.getClass().getSimpleName()));
                    }
                    // LWW: overwrite with new nested object
                    Map<String, Object> nested = new LinkedHashMap<>();
                    current.put(segment, nested);
                    current = nested;
                }
            }

            // Set final value
            String finalSegment = segments[segments.length - 1];
            Object existing = current.get(finalSegment);

            checkFinalValueConflict(finalSegment, existing, value);

            // LWW: last write wins (always overwrite in non-strict, or if types match in
            // strict)
            current.put(finalSegment, value);
        }

        private void checkFinalValueConflict(String finalSegment, Object existing, Object value) {
            if (existing != null && options.strict()) {
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
         * Parses a key-value string into an Object, handling nested objects, empty
         * values, and primitives.
         *
         * @param value the value string to parse
         * @param depth the depth at which the key-value pair is located
         * @return the parsed value (Map, List, or primitive)
         */
        private Object parseKeyValue(String value, int depth) {
            // Check if next line is nested (deeper indentation)
            if (currentLine + 1 < lines.length) {
                int nextDepth = getDepth(lines[currentLine + 1]);
                if (nextDepth > depth) {
                    currentLine++;
                    // parseNestedObject manages currentLine, so we don't increment here
                    return parseNestedObject(depth);
                } else {
                    // If value is empty, create empty object; otherwise parse as primitive
                    Object parsedValue;
                    if (value.trim().isEmpty()) {
                        parsedValue = new LinkedHashMap<>();
                    } else {
                        parsedValue = PrimitiveDecoder.parse(value);
                    }
                    currentLine++;
                    return parsedValue;
                }
            } else {
                // If value is empty, create empty object; otherwise parse as primitive
                Object parsedValue;
                if (value.trim().isEmpty()) {
                    parsedValue = new LinkedHashMap<>();
                } else {
                    parsedValue = PrimitiveDecoder.parse(value);
                }
                currentLine++;
                return parsedValue;
            }
        }

        /**
         * Puts a key-value pair into a map, handling path expansion.
         *
         * @param map          the map to put the key-value pair into
         * @param originalKey  the original key before being unescaped (used for path
         *                     expansion check)
         * @param unescapedKey the unescaped key
         * @param value        the value to put
         */
        private void putKeyValueIntoMap(Map<String, Object> map, String originalKey, String unescapedKey,
                                        Object value) {
            // Handle path expansion
            if (shouldExpandKey(originalKey)) {
                expandPathIntoMap(map, unescapedKey, value);
            } else {
                checkPathExpansionConflict(map, unescapedKey, value);
                map.put(unescapedKey, value);
            }
        }

        /**
         * Parses a key-value pair at root level, creating a new Map.
         */
        private Object parseKeyValuePair(String key, String value, int depth, boolean parseRootFields) {
            Map<String, Object> obj = new LinkedHashMap<>();
            parseKeyValuePairIntoMap(obj, key, value, depth);

            if (parseRootFields) {
                parseRootObjectFields(obj, depth);
            }
            return obj;
        }

        /**
         * Parses a key-value pair and adds it to an existing map.
         */
        private void parseKeyValuePairIntoMap(Map<String, Object> map, String key, String value, int depth) {
            String unescapedKey = StringEscaper.unescape(key);

            Object parsedValue = parseKeyValue(value, depth);
            putKeyValueIntoMap(map, key, unescapedKey, parsedValue);
        }

        /**
         * Checks for path expansion conflicts when setting a non-expanded key.
         * In strict mode, throws if the key conflicts with an existing expanded path.
         */
        private void checkPathExpansionConflict(Map<String, Object> map, String key, Object value) {
            if (!options.strict()) {
                return;
            }

            Object existing = map.get(key);
            checkFinalValueConflict(key, existing, value);
        }

        /**
         * Finds the index of the first unquoted colon in a line.
         * Critical for handling quoted keys like "order:id": value.
         */
        private int findUnquotedColon(String content) {
            boolean inQuotes = false;
            boolean escaped = false;

            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);

                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ':' && !inQuotes) {
                    return i;
                }
            }

            return -1;
        }

        /**
         * Calculates indentation depth (nesting level) of a line.
         * Counts leading spaces in multiples of the configured indent size.
         * In strict mode, validates indentation (no tabs, proper multiples).
         */
        private int getDepth(String line) {
            // Blank lines (including lines with only spaces) have depth 0
            if (isBlankLine(line)) {
                return 0;
            }

            // Validate indentation (including tabs) in strict mode
            // Check for tabs first before any other processing
            if (options.strict() && !line.isEmpty() && line.charAt(0) == '\t') {
                throw new IllegalArgumentException(
                        String.format("Tab character used in indentation at line %d", currentLine + 1));
            }

            if (options.strict()) {
                validateIndentation(line);
            }

            int depth;
            int indentSize = options.indent();
            int leadingSpaces = 0;

            // Count leading spaces
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ' ') {
                    leadingSpaces++;
                } else {
                    break;
                }
            }

            // Calculate depth based on indent size
            depth = leadingSpaces / indentSize;

            // In strict mode, check if it's an exact multiple
            if (options.strict() && leadingSpaces > 0
                    && leadingSpaces % indentSize != 0) {
                throw new IllegalArgumentException(
                        String.format("Non-multiple indentation: %d spaces with indent=%d at line %d",
                                leadingSpaces, indentSize, currentLine + 1));
            }

            return depth;
        }

        /**
         * Validates indentation in strict mode.
         * Checks for tabs, mixed tabs/spaces, and non-multiple indentation.
         */
        private void validateIndentation(String line) {
            if (line.trim().isEmpty()) {
                // Blank lines are allowed (handled separately)
                return;
            }

            int indentSize = options.indent();
            int leadingSpaces = 0;

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '\t') {
                    throw new IllegalArgumentException(
                            String.format("Tab character used in indentation at line %d", currentLine + 1));
                } else if (c == ' ') {
                    leadingSpaces++;
                } else {
                    // Reached non-whitespace
                    break;
                }
            }

            // Check for non-multiple indentation (only if there's actual content)
            if (leadingSpaces > 0 && leadingSpaces % indentSize != 0) {
                throw new IllegalArgumentException(
                        String.format("Non-multiple indentation: %d spaces with indent=%d at line %d",
                                leadingSpaces, indentSize, currentLine + 1));
            }
        }
    }
}