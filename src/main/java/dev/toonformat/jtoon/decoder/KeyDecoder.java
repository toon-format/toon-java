package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.PathExpansion;
import dev.toonformat.jtoon.util.StringEscaper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import static dev.toonformat.jtoon.util.Constants.DOT;
import static dev.toonformat.jtoon.util.Headers.KEYED_ARRAY_PATTERN;

/**
 * Handles decoding of key values/arrays to JSON format.
 */
public final class KeyDecoder {

    private KeyDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Processes a keyed array line (e.g., "key[3]: value").
     *
     * @param result      result
     * @param content     the content string to parse
     * @param originalKey the original Key
     * @param parentDepth parent depth of keyed array line
     * @param context     decode an object to deal with lines, delimiter and options
     */
    static void processKeyedArrayLine(final Map<String, Object> result, final String content, final String originalKey,
                                      final int parentDepth, final DecodeContext context) {
        final String key = StringEscaper.unescape(originalKey);
        final String arrayHeader = content.substring(originalKey.length());
        final List<Object> arrayValue = ArrayDecoder.parseArray(arrayHeader, parentDepth + 1, context);

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
    static void expandPathIntoMap(final Map<String, Object> current, final String dottedKey, final Object value,
                                  final DecodeContext context) {
        final String[] segments = dottedKey.split("\\.");
        Map<String, Object> currentMap = current;

        // Navigate/create nested structure
        for (int i = 0; i < segments.length - 1; i++) {
            final String segment = segments[i];
            final Object existing = currentMap.get(segment);

            if (existing == null) {
                // Create a new nested object
                final Map<String, Object> nested = new LinkedHashMap<>();
                currentMap.put(segment, nested);
                currentMap = nested;
            } else if (existing instanceof Map) {
                // Use existing nested object
                @SuppressWarnings("unchecked")
                final Map<String, Object> existingMap = (Map<String, Object>) existing;
                currentMap = existingMap;
            } else {
                // Conflict: existing is not a Map
                if (context.options.strict()) {
                    throw new IllegalArgumentException(
                        String.format("Path expansion conflict: %s is %s, cannot expand to object",
                            segment, existing.getClass().getSimpleName()));
                }
                // LWW: overwrite with new nested object
                final Map<String, Object> nested = new LinkedHashMap<>();
                currentMap.put(segment, nested);
                currentMap = nested;
            }
        }

        // Set the final value
        final String finalSegment = segments[segments.length - 1];
        final Object existing = currentMap.get(finalSegment);

        DecodeHelper.checkFinalValueConflict(finalSegment, existing, value, context);

        // LWW: last write wins (always overwrite in non-strict, or if types match in
        // strict)
        currentMap.put(finalSegment, value);
    }



    /**
     * Processes a key-value line (e.g., "key: value").
     *
     * @param result  result
     * @param content the content string to parse
     * @param depth   the depth of the value line
     * @param context decode an object to deal with lines, delimiter and options
     */
    static void processKeyValueLine(final Map<String, Object> result, final String content,
            final int depth, final DecodeContext context) {
        final int colonIdx = DecodeHelper.findUnquotedColon(content);

        if (colonIdx > 0) {
            final String key = content.substring(0, colonIdx).trim();
            final String value = content.substring(colonIdx + 1).trim();
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
    static void parseKeyValuePairIntoMap(final Map<String, Object> map, final String key, final String value,
                                         final int depth, final DecodeContext context) {
        final String unescapedKey = StringEscaper.unescape(key);

        final Object parsedValue = parseKeyValue(value, depth, context);
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
    static boolean shouldExpandKey(final String key, final DecodeContext context) {
        if (context.options.expandPaths() != PathExpansion.SAFE) {
            return false;
        }
        // Quoted keys should not be expanded
        if (key.trim().startsWith("\"") && key.trim().endsWith("\"")) {
            return false;
        }
        // Check if a key contains dots and is a valid identifier pattern
        if (!key.contains(DOT)) {
            return false;
        }
        // Valid identifier: starts with a letter or underscore, followed by letters,
        // digits, underscores
        // Each segment must match this pattern
        final String[] segments = key.split("\\.");
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
    private static Object parseKeyValue(final String value, final int depth, final DecodeContext context) {
        // Check if the next line is nested (deeper indentation)
        if (context.currentLine + 1 < context.lines.length) {
            final int nextDepth = DecodeHelper.getDepth(context.lines[context.currentLine + 1], context);
            if (nextDepth > depth) {
                context.currentLine++;
                // parseNestedObject manages the currentLine, so we don't increment here
                return ObjectDecoder.parseNestedObject(depth, context);
            } else {
                // If the value is empty, create an empty object; otherwise parse as primitive
                final Object parsedValue;
                if (value.isBlank()) {
                    parsedValue = new LinkedHashMap<>();
                } else {
                    parsedValue = PrimitiveDecoder.parse(value);
                }
                context.currentLine++;
                return parsedValue;
            }
        } else {
            // If the value is empty, create an empty object; otherwise parse as primitive
            final Object parsedValue;
            if (value.isBlank()) {
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
    private static void putKeyValueIntoMap(final Map<String, Object> map, final String originalKey,
            final String unescapedKey, final Object value, final DecodeContext context) {
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
    static Object parseKeyValuePair(final String key, final String value, final int depth,
            final boolean parseRootFields, final DecodeContext context) {
        final Map<String, Object> obj = new LinkedHashMap<>();
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
    static Object parseKeyedArrayValue(final MatchResult keyedArray, final String content,
            final int depth, final DecodeContext context) {
        final String group1 = keyedArray.group(1);
        final String originalKey = group1.trim();
        final String key = StringEscaper.unescape(originalKey);
        final String arrayHeader = content.substring(group1.length());

        final List<Object> arrayValue = ArrayDecoder.parseArray(arrayHeader, depth, context);
        final Map<String, Object> obj = new LinkedHashMap<>();

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
    static boolean parseKeyedArrayField(final String fieldContent, final Map<String, Object> item, final int depth,
                                        final DecodeContext context) {
        final Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(fieldContent);
        if (!keyedArray.matches()) {
            return false;
        }

        final String group1 = keyedArray.group(1);
        final String originalKey = group1.trim();
        final String key = StringEscaper.unescape(originalKey);
        final String arrayHeader = fieldContent.substring(group1.length());

        // For nested arrays in list items, default to comma delimiter if not specified
        final Delimiter nestedArrayDelimiter = ArrayDecoder.extractDelimiterFromHeader(arrayHeader, context);
        final List<Object> arrayValue = ArrayDecoder.parseArrayWithDelimiter(arrayHeader, depth + 2,
                                                                             nestedArrayDelimiter, context);

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
    static boolean parseKeyValueField(final String fieldContent, final Map<String, Object> item, final int depth,
                                      final DecodeContext context) {
        final int colonIdx = DecodeHelper.findUnquotedColon(fieldContent);
        if (colonIdx <= 0) {
            return false;
        }

        final String fieldKey = StringEscaper.unescape(fieldContent.substring(0, colonIdx).trim());
        final String fieldValue = fieldContent.substring(colonIdx + 1).trim();

        final Object parsedValue = ObjectDecoder.parseFieldValue(fieldValue, depth + 2, context);

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
