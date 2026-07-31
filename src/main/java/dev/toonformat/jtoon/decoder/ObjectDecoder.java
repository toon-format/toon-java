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

            if (depth == parentDepth + 1) {
                processDirectChildLine(result, line, parentDepth, depth, context);
            } else if (depth > parentDepth + 1) {
                // Over-indented line jumps past the expected depth (§14.2)
                if (context.options.strict()) {
                    throw new IllegalArgumentException(
                        "Over-indented line at " + (context.currentLine + 1) + " (depth " + depth + ")");
                }
                context.currentLine++;
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
    private static void processDirectChildLine(final Map<String, Object> result, final String line,
            final int parentDepth, final int depth, final DecodeContext context) {
        final String content = line.substring((parentDepth + 1) * context.options.indent());

        // Spec §5/§6: keyless array headers are valid only as the document's
        // root header or as list items; an object field position is a defect.
        if (content.startsWith(OPEN_BRACKET) && context.options.strict()) {
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
        while (isRootFieldLine(depth, context)) {
            final String line = context.lines[context.currentLine];

            // Skip blank lines
            if (DecodeHelper.isBlankLine(line)) {
                context.currentLine++;
                continue;
            }

            final String content = line.substring(depth * context.options.indent());

            if (!processRootFieldLine(obj, content, depth, context)) {
                return;
            }
        }
    }

    /**
     * Returns whether the current line is a root field line at the given
     * depth, staying within the line buffer.
     *
     * @param depth   the expected root field depth
     * @param context decode an object to deal with lines, delimiter and options
     * @return true when the current line sits at the root field depth
     */
    private static boolean isRootFieldLine(final int depth, final DecodeContext context) {
        return context.currentLine < context.lines.length
            && DecodeHelper.getDepth(context.lines[context.currentLine], context) == depth;
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
        // it is a defect.
        if (content.startsWith(OPEN_BRACKET) && context.options.strict()) {
            throw new IllegalArgumentException(
                "Keyless array header only valid as root header at line " + (context.currentLine + 1));
        }

        final Headers.KeyedHeaderMatch keyedHeader = Headers.matchKeyedArrayHeader(content);
        if (keyedHeader != null) {
            processRootKeyedArrayLine(obj, content, keyedHeader, depth, context);
            return true;
        }

        final int colonIdx = DecodeHelper.findUnquotedColon(content);
        if (colonIdx > 0) {
            final String key = content.substring(0, colonIdx).trim();
            final String value = content.substring(colonIdx + 1).trim();

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

        final String originalKey = keyedHeader.key();
        final String originalKeyTrimmed = originalKey.trim();
        final String key = StringEscaper.unescape(originalKey);
        final String arrayHeader = content.substring(originalKey.length());

        // Spec §6: a keyed tabular header whose bracket and brace segments
        // declare different delimiters is defective; in non-strict mode the
        // whole line falls through and decodes as an ordinary key-value pair.
        if (!context.options.strict() && ArrayDecoder.hasTabularDelimiterMismatch(arrayHeader)) {
            final int colonIdx = DecodeHelper.findUnquotedColon(content);
            if (colonIdx > 0) {
                KeyDecoder.parseKeyValuePairIntoMap(objectMap,
                    content.substring(0, colonIdx).trim(),
                    content.substring(colonIdx + 1).trim(), depth, context);
                return;
            }
        }

        final List<Object> arrayValue = ArrayDecoder.parseArray(arrayHeader, depth, context);

        // Handle path expansion for array keys
        if (KeyDecoder.shouldExpandKey(originalKeyTrimmed, context)) {
            KeyDecoder.expandPathIntoMap(objectMap, key, arrayValue, context);
        } else {
            // Check for conflicts with existing expanded paths
            DecodeHelper.checkPathExpansionConflict(objectMap, key, arrayValue, context);
            DecodeHelper.checkDuplicateKey(objectMap, key, context);
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
    static Object parseBareScalarValue(final String content, final int depth, final DecodeContext context) {
        final Object result = PrimitiveDecoder.parse(content, context);
        context.currentLine++;

        // In strict mode, check if there are more primitives at the root level
        if (depth == 0 && context.options.strict()) {
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
        if (value.isBlank()) {
            return new LinkedHashMap<>();
        }
        return PrimitiveDecoder.parse(value, context);
    }

    /**
     * Parses a value that may either open a nested scope or decode as a
     * scalar. Deeper lines open a nested object; inline values on a field
     * that does not open a scope are rejected in strict mode (§14.2).
     *
     * @param value        the value string to parse
     * @param depth        the depth at which the value is located
     * @param context      decode an object to deal with lines, delimiter and options
     * @param scalarParser parses the value when it does not open a scope
     * @return the parsed value (Map or scalar)
     */
    static Object parseValueWithNestedScope(final String value, final int depth, final DecodeContext context,
            final BiFunction<String, DecodeContext, Object> scalarParser) {
        // Check if the next line is nested (deeper indentation)
        if (context.currentLine + 1 < context.lines.length) {
            final int nextDepth = DecodeHelper.getDepth(context.lines[context.currentLine + 1], context);
            if (nextDepth > depth) {
                if (!value.isBlank()) {
                    return parseInlineValueWithOrphanLines(value, depth, nextDepth, context, scalarParser);
                }
                context.currentLine++;
                // parseNestedObject manages the currentLine, so we don't increment here
                return parseNestedObject(depth, context);
            }
            context.currentLine++;
            return scalarParser.apply(value, context);
        }
        context.currentLine++;
        return scalarParser.apply(value, context);
    }

    /**
     * Parses an inline value whose line carries deeper, orphaned lines:
     * rejected in strict mode (§14.2), skipped in non-strict mode.
     *
     * @param value        the inline value string to parse
     * @param depth        the depth at which the value is located
     * @param nextDepth    the depth of the first orphaned line
     * @param context      decode an object to deal with lines, delimiter and options
     * @param scalarParser parses the inline value
     * @return the parsed scalar value
     */
    private static Object parseInlineValueWithOrphanLines(final String value, final int depth, final int nextDepth,
            final DecodeContext context, final BiFunction<String, DecodeContext, Object> scalarParser) {
        // Inline value: the field does not open a scope, so a deeper
        // line belongs to no scope at all (§14.2)
        if (context.options.strict()) {
            throw new IllegalArgumentException(
                "Over-indented line at " + (context.currentLine + 2) + " (depth " + nextDepth + ")");
        }
        // Non-strict: skip the orphaned lines and keep the inline value
        do {
            context.currentLine++;
        } while (context.currentLine < context.lines.length
            && DecodeHelper.getDepth(context.lines[context.currentLine], context) > depth);
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
        final boolean isEmpty = value.isBlank();

        // Find the next non-blank line and its depth
        final Integer nextDepth = DecodeHelper.findNextNonBlankLineDepth(context);
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
        return isEmpty ? new LinkedHashMap<>() : PrimitiveDecoder.parse(value, context);
    }
}
