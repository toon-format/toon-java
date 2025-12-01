package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.PathExpansion;
import dev.toonformat.jtoon.util.StringEscaper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static dev.toonformat.jtoon.util.Headers.KEYED_ARRAY_PATTERN;

/**
 * Handles decoding of key values/arrays to JSON format.
 */
public class KeyDecoder {

    private KeyDecoder() {throw new UnsupportedOperationException("Utility class cannot be instantiated");}

    /**
     * Processes a keyed array line (e.g., "key[3]: value").
     *
     * @param result      result
     * @param content     the content string to parse
     * @param originalKey the original Key
     * @param parentDepth parent depth of keyed array line
     * @param context     decode an object to deal with lines, delimiter and options
     */
    protected static void processKeyedArrayLine(Map<String, Object> result, String content, String originalKey,
                                                int parentDepth, DecodeContext context) {
        String key = StringEscaper.unescape(originalKey);
        String arrayHeader = content.substring(originalKey.length());
        List<Object> arrayValue = ArrayDecoder.parseArray(arrayHeader, parentDepth + 1, context);

        // Handle path expansion for array keys
        if (shouldExpandKey(originalKey, context)) {
            expandPathIntoMap(result, key, arrayValue, context);
        } else {
            // Check for conflicts with existing expanded paths
            DecodeHelper.checkPathExpansionConflict(result, key, arrayValue, context);
            result.put(key, arrayValue);
        }
    }

    /**
     * Expands a dotted key into a nested object structure.
     *
     * @param current   map
     * @param dottedKey dottedKey
     * @param value     value
     * @param context   decode an object to deal with lines, delimiter and options
     */
    protected static void expandPathIntoMap(Map<String, Object> current, String dottedKey, Object value, DecodeContext context) {
        String[] segments = dottedKey.split("\\.");

        // Navigate/create nested structure
        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            Object existing = current.get(segment);

            if (existing == null) {
                // Create a new nested object
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
                if (context.options.strict()) {
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

        // Set the final value
        String finalSegment = segments[segments.length - 1];
        Object existing = current.get(finalSegment);

        DecodeHelper.checkFinalValueConflict(finalSegment, existing, value, context);

        // LWW: last write wins (always overwrite in non-strict, or if types match in
        // strict)
        current.put(finalSegment, value);
    }

    /**
     * Processes a key-value line (e.g., "key: value").
     *
     * @param result  result
     * @param content the content string to parse
     * @param depth   the depth of the value line
     * @param context decode an object to deal with lines, delimiter and options
     */
    protected static void processKeyValueLine(Map<String, Object> result, String content, int depth, DecodeContext context) {
        int colonIdx = DecodeHelper.findUnquotedColon(content);

        if (colonIdx > 0) {
            String key = content.substring(0, colonIdx).trim();
            String value = content.substring(colonIdx + 1).trim();
            parseKeyValuePairIntoMap(result, key, value, depth, context);
        } else {
            // No colon found in key-value context - this is an error
            if (context.options.strict()) {
                throw new IllegalArgumentException(
                    "Missing colon in key-value context at line " + (context.currentLine + 1));
            }
            context.currentLine++;
        }
    }

    /**
     * Parses a key-value pair and adds it to an existing map.
     *
     * @param map     existing map
     * @param key     key
     * @param value   the value string to parse
     * @param depth   the depth of the value pair
     * @param context decode an object to deal with lines, delimiter and options
     */
    protected static void parseKeyValuePairIntoMap(Map<String, Object> map, String key, String value,
                                                   int depth, DecodeContext context) {
        String unescapedKey = StringEscaper.unescape(key);

        Object parsedValue = parseKeyValue(value, depth, context);
        putKeyValueIntoMap(map, key, unescapedKey, parsedValue, context);
    }

    /**
     * Checks if a key should be expanded (is a valid identifier segment).
     * Keys with dots that are valid identifiers can be expanded.
     * Quoted keys are never expanded.
     *
     * @param key     key
     * @param context decode an object to deal with lines, delimiter and options
     * @return true if a key should be expanded or false if not
     */
    protected static boolean shouldExpandKey(String key, DecodeContext context) {
        if (context.options.expandPaths() != PathExpansion.SAFE) {
            return false;
        }
        // Quoted keys should not be expanded
        if (key.trim().startsWith("\"") && key.trim().endsWith("\"")) {
            return false;
        }
        // Check if a key contains dots and is a valid identifier pattern
        if (!key.contains(".")) {
            return false;
        }
        // Valid identifier: starts with a letter or underscore, followed by letters,
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
     * Parses a key-value string into an Object, handling nested objects,
     * empty values, and primitives.
     *
     * @param value the value string to parse
     * @param depth the depth at which the key-value pair is located
     * @return the parsed value (Map, List, or primitive)
     */
    private static Object parseKeyValue(String value, int depth, DecodeContext context) {
        // Check if the next line is nested (deeper indentation)
        if (context.currentLine + 1 < context.lines.length) {
            int nextDepth = DecodeHelper.getDepth(context.lines[context.currentLine + 1], context);
            if (nextDepth > depth) {
                context.currentLine++;
                // parseNestedObject manages the currentLine, so we don't increment here
                return ObjectDecoder.parseNestedObject(depth, context);
            } else {
                // If the value is empty, create an empty object; otherwise parse as primitive
                Object parsedValue;
                if (value.trim().isEmpty()) {
                    parsedValue = new LinkedHashMap<>();
                } else {
                    parsedValue = PrimitiveDecoder.parse(value);
                }
                context.currentLine++;
                return parsedValue;
            }
        } else {
            // If the value is empty, create an empty object; otherwise parse as primitive
            Object parsedValue;
            if (value.trim().isEmpty()) {
                parsedValue = new LinkedHashMap<>();
            } else {
                parsedValue = PrimitiveDecoder.parse(value);
            }
            context.currentLine++;
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
    private static void putKeyValueIntoMap(Map<String, Object> map, String originalKey, String unescapedKey,
                                           Object value, DecodeContext context) {
        // Handle path expansion
        if (shouldExpandKey(originalKey, context)) {
            expandPathIntoMap(map, unescapedKey, value, context);
        } else {
            DecodeHelper.checkPathExpansionConflict(map, unescapedKey, value, context);
            map.put(unescapedKey, value);
        }
    }

    /**
     * Parses a key-value pair at root level, creating a new Map.
     *
     * @param key             key-value
     * @param value           the value string to parse
     * @param depth           the depth of the key value pair
     * @param parseRootFields true or false if root fields should be parsed
     * @param context         decode an object to deal with lines, delimiter, and options
     * @return parsed a key-value pair
     */
    protected static Object parseKeyValuePair(String key, String value, int depth, boolean parseRootFields,
                                              DecodeContext context) {
        Map<String, Object> obj = new LinkedHashMap<>();
        parseKeyValuePairIntoMap(obj, key, value, depth, context);

        if (parseRootFields) {
            ObjectDecoder.parseRootObjectFields(obj, depth, context);
        }
        return obj;
    }

    /**
     * Parses a keyed array value (e.g., "items[2]{id,name}:").
     *
     * @param keyedArray keyed array
     * @param content    the content string to parse
     * @param depth      the depth of the keyed array value
     * @param context    decode an object to deal with lines, delimiter, and options
     * @return parsed keyed array value
     */
    protected static Object parseKeyedArrayValue(Matcher keyedArray, String content, int depth, DecodeContext context) {
        String originalKey = keyedArray.group(1).trim();
        String key = StringEscaper.unescape(originalKey);
        String arrayHeader = content.substring(keyedArray.group(1).length());

        var arrayValue = ArrayDecoder.parseArray(arrayHeader, depth, context);
        Map<String, Object> obj = new LinkedHashMap<>();

        // Handle path expansion for array keys
        if (shouldExpandKey(originalKey, context)) {
            expandPathIntoMap(obj, key, arrayValue, context);
        } else {
            // Check for conflicts with existing expanded paths
            DecodeHelper.checkPathExpansionConflict(obj, key, arrayValue, context);
            obj.put(key, arrayValue);
        }

        // Continue parsing root-level fields if at depth 0
        if (depth == 0) {
            ObjectDecoder.parseRootObjectFields(obj, depth, context);
        }

        return obj;
    }

    /**
     * Parses a keyed array field and adds it to the item map.
     *
     * @param fieldContent the field content to parse
     * @param item         the map to add the field to
     * @param depth        the depth of the list item
     * @param context      decode an object to deal with lines, delimiter and options
     * @return true if the field was processed as a keyed array, false otherwise
     */
    protected static boolean parseKeyedArrayField(String fieldContent, Map<String, Object> item, int depth, DecodeContext context) {
        Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(fieldContent);
        if (!keyedArray.matches()) {
            return false;
        }

        String originalKey = keyedArray.group(1).trim();
        String key = StringEscaper.unescape(originalKey);
        String arrayHeader = fieldContent.substring(keyedArray.group(1).length());

        // For nested arrays in list items, default to comma delimiter if not specified
        String nestedArrayDelimiter = ArrayDecoder.extractDelimiterFromHeader(arrayHeader, context);
        var arrayValue = ArrayDecoder.parseArrayWithDelimiter(arrayHeader, depth + 2, nestedArrayDelimiter, context);

        // Handle path expansion for array keys
        if (shouldExpandKey(originalKey, context)) {
            expandPathIntoMap(item, key, arrayValue, context);
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
     * @param context      decode an object to deal with lines, delimiter and options
     * @return true if the field was processed as a key-value pair, false otherwise
     */
    protected static boolean parseKeyValueField(String fieldContent, Map<String, Object> item, int depth, DecodeContext context) {
        int colonIdx = DecodeHelper.findUnquotedColon(fieldContent);
        if (colonIdx <= 0) {
            return false;
        }

        String fieldKey = StringEscaper.unescape(fieldContent.substring(0, colonIdx).trim());
        String fieldValue = fieldContent.substring(colonIdx + 1).trim();

        Object parsedValue = ObjectDecoder.parseFieldValue(fieldValue, depth + 2, context);

        // Handle path expansion
        if (shouldExpandKey(fieldKey, context)) {
            expandPathIntoMap(item, fieldKey, parsedValue, context);
        } else {
            item.put(fieldKey, parsedValue);
        }

        // parseFieldValue manages currentLine appropriately
        return true;
    }
}
