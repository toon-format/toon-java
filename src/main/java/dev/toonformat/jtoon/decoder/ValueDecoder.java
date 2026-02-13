package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.util.ObjectMapperSingleton;
import tools.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import static dev.toonformat.jtoon.util.Constants.NULL_LITERAL;
import static dev.toonformat.jtoon.util.Constants.OPEN_BRACKET;
import static dev.toonformat.jtoon.util.Headers.KEYED_ARRAY_PATTERN;

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

    private static final ObjectMapper MAPPER = ObjectMapperSingleton.getInstance();

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
    public static Object decode(final String toon, final DecodeOptions options) {
        if (toon == null || toon.isBlank()) {
            return new LinkedHashMap<>();
        }

        // Special case: if input is exactly "null", return null
        final String trimmed = toon.trim();
        if (NULL_LITERAL.equals(trimmed)) {
            return null;
        }

        // Don't trim leading whitespace - we need it for indentation validation
        // Only trim trailing whitespace to avoid issues with empty lines at the end
        final String processed = Character.isWhitespace(toon.charAt(toon.length() - 1))
            ? toon.stripTrailing()
            : toon;

        //set an own decode context
        final DecodeContext context = new DecodeContext();
        context.lines = processed.split("\r?\n", -1);
        context.options = options;
        context.delimiter = options.delimiter();

        final int lineIndex = context.currentLine;
        final String line = context.lines[lineIndex];
        final int depth = DecodeHelper.getDepth(line, context);

        if (depth > 0) {
            if (context.options.strict()) {
                throw new IllegalArgumentException("Unexpected indentation at line " + lineIndex);
            }
            return new LinkedHashMap<>();
        }

        // Handle standalone arrays: [2]:
        if (!line.isEmpty() && line.charAt(0) == OPEN_BRACKET.charAt(0)) {
            return ArrayDecoder.parseArray(line, depth, context);
        }

        // Handle keyed arrays: items[2]{id,name}:
        final Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(line);
        if (keyedArray.matches()) {
            return KeyDecoder.parseKeyedArrayValue(keyedArray, line, depth, context);
        }
        // Handle key-value pairs: name: Ada
        final int colonIdx = DecodeHelper.findUnquotedColon(line);
        if (colonIdx > 0) {
            final String key = line.substring(0, colonIdx).trim();
            final String value = line.substring(colonIdx + 1).trim();
            return KeyDecoder.parseKeyValuePair(key, value, depth, depth == 0, context);
        }

        // Bare scalar value
        return ObjectDecoder.parseBareScalarValue(line, depth, context);
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
    public static String decodeToJson(final String toon, final DecodeOptions options) {
        try {
            final Object decoded = decode(toon, options);
            return MAPPER.writeValueAsString(decoded);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert decoded value to JSON: " + e.getMessage(), e);
        }
    }
}
