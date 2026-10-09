package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.util.Headers;
import dev.toonformat.jtoon.util.StringEscaper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import static dev.toonformat.jtoon.util.Constants.OPEN_BRACKET;

/**
 * Handles decoding of TOON objects to JSON format.
 */
public final class ObjectDecoder {

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
    static Map<String, Object> parseNestedObject(final int parentDepth, final DecodeContext context) {
        context.incrementDepth();
        try {
            return doParseNestedObject(parentDepth, context);
        } finally {
            context.decrementDepth();
        }
    }

    private static Map<String, Object> doParseNestedObject(final int parentDepth, final DecodeContext context) {
        final Map<String, Object> result = new LinkedHashMap<>();
        final int contentDepth = DecodeHelper.findContentDepth(parentDepth, context);

        while (context.currentLine < context.lines.length) {
            final String line = context.lines[context.currentLine];

            // Skip blank lines
            if (DecodeHelper.isBlankLine(line)) {
                context.currentLine++;
                continue;
            }

            final int depth = DecodeHelper.getDepth(line, context);

            if (depth <= parentDepth) {
                return result;
            }

            // A line off the content depth belongs to no scope (§14.2)
            if (depth != contentDepth) {
                throw DecodeHelper.overIndentedLineError(context, depth);
            }
            processDirectChildLine(result, line, contentDepth - 1, depth, context);
        }

        return result;
    }

    /**
     * Processes a line at depth == parentDepth + 1 (direct child).
     * Returns true if the line was processed, false if it was a blank line that was
     * skipped.
     */
    private static void processDirectChildLine(final Map<String, Object> result, final String line,
            final int parentDepth, final int depth, final DecodeContext context) {
        final String content = line.substring((parentDepth + 1) * context.options.indent());

        // Spec §5/§6: keyless array headers are valid only as the document's
        // root header or as list items; an object field position is a defect
        // in both modes (§14.2).
        if (content.startsWith(OPEN_BRACKET)) {
            throw new IllegalArgumentException(
                "Keyless array header only valid at document root at line " + (context.currentLine + 1));
        }

        final Headers.KeyedHeaderMatch keyedHeader = Headers.matchKeyedArrayHeader(content);

        if (keyedHeader != null) {
            KeyDecoder.processKeyedArrayLine(result, content, keyedHeader, parentDepth, context);
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
    static void parseRootObjectFields(final Map<String, Object> obj, final int depth, final DecodeContext context) {
        while (context.currentLine < context.lines.length) {
            final String line = context.lines[context.currentLine];

            // Skip blank lines
            if (DecodeHelper.isBlankLine(line)) {
                context.currentLine++;
                continue;
            }

            final int lineDepth = DecodeHelper.getDepth(line, context);
            if (lineDepth < depth) {
                return;
            }
            // A deeper line belongs to no field (§8, §14.2)
            if (lineDepth > depth) {
                throw DecodeHelper.overIndentedLineError(context, lineDepth);
            }

            final String content = line.substring(depth * context.options.indent());

            if (!processRootFieldLine(obj, content, depth, context)) {
                return;
            }
        }
    }

    /**
     * Processes a single root field line. Returns false when the line is not
     * a root field, terminating the root field scan.
     *
     * @param obj     the string key-value pairs
     * @param content the content string to parse
     * @param depth   the depth of the object field
     * @param context decode an object to deal with lines, delimiter and options
     * @return false when the line ends the root field scan
     */
    private static boolean processRootFieldLine(final Map<String, Object> obj, final String content,
            final int depth, final DecodeContext context) {
        // Spec §5/§6: a keyless header is only valid as the document's
        // root header, i.e. the first line; at any later depth-0 position
        // it is a defect in both modes (§14.2).
        if (content.startsWith(OPEN_BRACKET)) {
            throw new IllegalArgumentException(
                "Keyless array header only valid as root header at line " + (context.currentLine + 1));
        }

        final Headers.KeyedHeaderMatch keyedHeader = Headers.matchKeyedArrayHeader(content);
        if (keyedHeader != null) {
            processRootKeyedArrayLine(obj, content, keyedHeader, depth, context);
            return true;
        }

        final int colonIdx = DecodeHelper.findUnquotedColon(content);
        if (colonIdx >= 0) {
            DecodeHelper.rejectMalformedHeaderLine(content, context);
            final String key = DecodeHelper.trimSpaces(content.substring(0, colonIdx));
            final String value = DecodeHelper.trimSpaces(content.substring(colonIdx + 1));

            KeyDecoder.parseKeyValuePairIntoMap(obj, key, value, depth, context);
            return true;
        }
        return false;
    }

    /**
     * Processes a keyed array line in root object fields.
     *
     * @param objectMap   the string key-value pairs
     * @param content     the content string to parse
     * @param keyedHeader the keyed header match for the content
     * @param depth       the depth of the object field
     * @param context     decode an object to deal with lines, delimiter and options
     */
    private static void processRootKeyedArrayLine(final Map<String, Object> objectMap,
            final String content, final Headers.KeyedHeaderMatch keyedHeader, final int depth,
            final DecodeContext context) {
        if (keyedHeader.keyed()) {
            final Object keyedValue = KeyedObjectDecoder.parseKeyedTabularObject(content, keyedHeader, depth + 1,
                    context);
            KeyDecoder.putKeyedValueIntoMap(objectMap, keyedHeader, keyedValue, context);
            return;
        }

        final String originalKey = DecodeHelper.trimSpaces(keyedHeader.key());
        final String key = StringEscaper.unescape(originalKey);
        final String arrayHeader = content.substring(keyedHeader.key().length());

        final List<Object> arrayValue = ArrayDecoder.parseArray(arrayHeader, depth, context);

        // Handle path expansion for array keys
        if (KeyDecoder.shouldExpandKey(originalKey, context)) {
            KeyDecoder.expandPathIntoMap(objectMap, key, arrayValue, context);
        } else {
            // Check for conflicts with existing expanded paths
            DecodeHelper.checkPathExpansionConflict(objectMap, key, arrayValue, context);
            DecodeHelper.checkDuplicateKey(objectMap, key, context);
            objectMap.put(key, arrayValue);
        }
    }

    /**
     * Parses a bare scalar value.
     *
     * @param content the content string to parse
     * @param context decode an object to deal with lines, delimiter and options
     * @return the parsed scalar value
     */
    static Object parseBareScalarValue(final String content, final DecodeContext context) {
        final Object result = PrimitiveDecoder.parse(content, context);
        context.currentLine++;
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
    static Object parseFieldValue(final String fieldValue, final int fieldDepth, final DecodeContext context) {
        return parseValueWithNestedScope(fieldValue, fieldDepth, context, ObjectDecoder::parseFieldScalar);
    }

    /**
     * Parses a field value that does not open a nested scope: a blank value
     * becomes an empty object, any other value a primitive.
     *
     * @param value   the value string to parse
     * @param context decode an object to deal with lines, delimiter and options
     * @return the parsed value (Map or primitive)
     */
    private static Object parseFieldScalar(final String value, final DecodeContext context) {
        if (value.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return PrimitiveDecoder.parse(value, context);
    }

    /**
     * Parses a value that may either open a nested scope or decode as a
     * scalar. Deeper lines open a nested object; a deeper line right after
     * an inline value belongs to no scope and is rejected in both modes (§14.2).
     *
     * @param value        the value string to parse
     * @param depth        the depth at which the value is located
     * @param context      decode an object to deal with lines, delimiter and options
     * @param scalarParser parses the value when it does not open a scope
     * @return the parsed value (Map or scalar)
     */
    static Object parseValueWithNestedScope(final String value, final int depth, final DecodeContext context,
            final BiFunction<String, DecodeContext, Object> scalarParser) {
        // Blank lines never create or close structure (§12), so the decision
        // looks at the first non-blank line after the field line.
        final int nextIdx = DecodeHelper.findNextNonBlankLine(context.currentLine + 1, context);
        if (nextIdx < context.lines.length
                && DecodeHelper.getDepth(context.lines[nextIdx], context) > depth) {
            context.currentLine++;
            if (value.isEmpty()) {
                // parseNestedObject manages the currentLine, so we don't increment here
                return parseNestedObject(depth, context);
            }
            // Inline value: the field does not open a scope, so a deeper
            // line belongs to no scope at all (§14.2)
            final int lineDepth = DecodeHelper.getDepth(context.lines[context.currentLine], context);
            if (lineDepth > depth) {
                throw DecodeHelper.overIndentedLineError(context, lineDepth);
            }
            return scalarParser.apply(value, context);
        }
        context.currentLine++;
        return scalarParser.apply(value, context);
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
    static Object parseObjectItemValue(final String value, final int depth, final DecodeContext context) {
        final boolean isEmpty = value.isEmpty();

        // Find the next non-blank line and its depth
        final Integer nextDepth = DecodeHelper.findNextNonBlankLineDepth(context);

        // Handle empty value with nested content.
        // The list item is at depth, and the field itself is conceptually at depth + 1,
        // So nested content should be parsed with parentDepth = depth + 1
        // This allows nested fields at depth + 2 or deeper to be processed correctly
        if (isEmpty && nextDepth != null && nextDepth > depth) {
            return parseNestedObject(depth + 1, context);
        }

        // Handle empty value without nested content or non-empty value
        return isEmpty ? new LinkedHashMap<>() : PrimitiveDecoder.parse(value, context);
    }
}
