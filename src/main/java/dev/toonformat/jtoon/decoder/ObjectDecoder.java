package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.util.StringEscaper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static dev.toonformat.jtoon.util.Headers.KEYED_ARRAY_PATTERN;

/**
 * Handles decoding of TOON objects to JSON format.
 */
public class ObjectDecoder {

    private ObjectDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parses nested object starting at the currentLine.
     *
     * @param parentDepth the parent depth of the nested object
     * @param context     decode an object to deal with lines, delimiter and options
     * @return parsed nested object
     */
    protected static Map<String, Object> parseNestedObject(int parentDepth, DecodeContext context) {
        Map<String, Object> result = new LinkedHashMap<>();

        while (context.currentLine < context.lines.length) {
            String line = context.lines[context.currentLine];

            // Skip blank lines
            if (DecodeHelper.isBlankLine(line)) {
                context.currentLine++;
                continue;
            }

            int depth = DecodeHelper.getDepth(line, context);

            if (depth <= parentDepth) {
                return result;
            }

            if (depth == parentDepth + 1) {
                processDirectChildLine(result, line, parentDepth, depth, context);
            } else {
                context.currentLine++;
            }
        }

        return result;
    }

    /**
     * Processes a line at depth == parentDepth + 1 (direct child).
     * Returns true if the line was processed, false if it was a blank line that was
     * skipped.
     */
    private static void processDirectChildLine(Map<String, Object> result, String line, int parentDepth, int depth, DecodeContext context) {
        String content = line.substring((parentDepth + 1) * context.options.indent());
        Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);

        if (keyedArray.find()) {
            KeyDecoder.processKeyedArrayLine(result, content, keyedArray.group(1), parentDepth, context);
        } else {
            KeyDecoder.processKeyValueLine(result, content, depth, context);
        }
    }

    /**
     * Parses additional key-value pairs at the root level.
     *
     * @param obj     the string key-value pairs
     * @param depth   the depth of the object field
     * @param context decode an object to deal with lines, delimiter and options
     */
    protected static void parseRootObjectFields(Map<String, Object> obj, int depth, DecodeContext context) {
        while (context.currentLine < context.lines.length) {
            String line = context.lines[context.currentLine];
            int lineDepth = DecodeHelper.getDepth(line, context);

            if (lineDepth != depth) {
                return;
            }

            // Skip blank lines
            if (DecodeHelper.isBlankLine(line)) {
                context.currentLine++;
                continue;
            }

            String content = line.substring(depth * context.options.indent());

            Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
            if (keyedArray.matches()) {
                processRootKeyedArrayLine(obj, content, keyedArray.group(1), depth, context);
            } else {
                int colonIdx = DecodeHelper.findUnquotedColon(content);
                if (colonIdx > 0) {
                    String key = content.substring(0, colonIdx).trim();
                    String value = content.substring(colonIdx + 1).trim();

                    KeyDecoder.parseKeyValuePairIntoMap(obj, key, value, depth, context);
                } else {
                    return;
                }
            }
        }
    }

    /**
     * Processes a keyed array line in root object fields.
     *
     * @param objectMap   the string key-value pairs
     * @param content     the content string to parse
     * @param originalKey the original Key
     * @param depth       the depth of the object field
     * @param context     decode an object to deal with lines, delimiter and options
     */
    private static void processRootKeyedArrayLine(Map<String, Object> objectMap, String content, String originalKey,
                                                  int depth, DecodeContext context) {
        String originalKeyTrimmed = originalKey.trim();
        String key = StringEscaper.unescape(originalKey);
        String arrayHeader = content.substring(originalKey.length());

        var arrayValue = ArrayDecoder.parseArray(arrayHeader, depth, context);

        // Handle path expansion for array keys
        if (KeyDecoder.shouldExpandKey(originalKeyTrimmed, context)) {
            KeyDecoder.expandPathIntoMap(objectMap, key, arrayValue, context);
        } else {
            // Check for conflicts with existing expanded paths
            DecodeHelper.checkPathExpansionConflict(objectMap, key, arrayValue, context);
            objectMap.put(key, arrayValue);
        }
    }

    /**
     * Parses a bare scalar value and validates in strict mode.
     *
     * @param content the content string to parse
     * @param depth   the depth of the scalar value
     * @param context decode an object to deal with lines, delimiter and options
     * @return the parsed scalar value
     */
    protected static Object parseBareScalarValue(String content, int depth, DecodeContext context) {
        Object result = PrimitiveDecoder.parse(content);
        context.currentLine++;

        // In strict mode, check if there are more primitives at the root level
        if (context.options.strict() && depth == 0) {
            DecodeHelper.validateNoMultiplePrimitivesAtRoot(context);
        }

        return result;
    }

    /**
     * Parses a field value, handling nested objects, empty values, and primitives.
     *
     * @param fieldValue the value string to parse
     * @param fieldDepth the depth at which the field is located
     * @param context    decode an object to deal with lines, delimiter and options
     * @return the parsed value (Map, List, or primitive)
     */
    protected static Object parseFieldValue(String fieldValue, int fieldDepth, DecodeContext context) {
        // Check if the next line is nested
        if (context.currentLine + 1 < context.lines.length) {
            int nextDepth = DecodeHelper.getDepth(context.lines[context.currentLine + 1], context);
            if (nextDepth > fieldDepth) {
                context.currentLine++;
                // parseNestedObject manages the currentLine, so we don't increment here
                return parseNestedObject(fieldDepth, context);
            } else {
                // If the value is empty, create an empty object; otherwise parse as primitive
                if (fieldValue.trim().isEmpty()) {
                    context.currentLine++;
                    return new LinkedHashMap<>();
                } else {
                    context.currentLine++;
                    return PrimitiveDecoder.parse(fieldValue);
                }
            }
        } else {
            // If the value is empty, create an empty object; otherwise parse as primitive
            if (fieldValue.trim().isEmpty()) {
                context.currentLine++;
                return new LinkedHashMap<>();
            } else {
                context.currentLine++;
                return PrimitiveDecoder.parse(fieldValue);
            }
        }
    }

    /**
     * Parses the value portion of an object item in a list, handling nested
     * objects,
     * empty values, and primitives.
     *
     * @param value   the value string to parse
     * @param depth   the depth of the list item
     * @param context decode an object to deal with lines, delimiter and options
     * @return the parsed value (Map, List, or primitive)
     */
    protected static Object parseObjectItemValue(String value, int depth, DecodeContext context) {
        boolean isEmpty = value.trim().isEmpty();

        // Find the next non-blank line and its depth
        Integer nextDepth = DecodeHelper.findNextNonBlankLineDepth(context);
        if (nextDepth == null) {
            // No non-blank line found - create an empty object
            return new LinkedHashMap<>();
        }

        // Handle empty value with nested content.
        // The list item is at depth, and the field itself is conceptually at depth + 1,
        // So nested content should be parsed with parentDepth = depth + 1
        // This allows nested fields at depth + 2 or deeper to be processed correctly
        if (isEmpty && nextDepth > depth) {
            return parseNestedObject(depth + 1, context);
        }

        // Handle empty value without nested content or non-empty value
        return isEmpty ? new LinkedHashMap<>() : PrimitiveDecoder.parse(value);
    }
}
