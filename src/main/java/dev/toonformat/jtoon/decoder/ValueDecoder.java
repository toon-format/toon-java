package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.util.ObjectMapperSingleton;
import org.jspecify.annotations.Nullable;
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
    @Nullable
    public static Object decode(final String toon, final DecodeOptions options) {
        try {
            return decodeInternal(toon, options);
        } catch (IllegalArgumentException e) {
            if (!options.strict()) {
                return null;
            }
            throw e;
        }
    }

    @Nullable
    private static Object decodeInternal(final String toon, final DecodeOptions options) {
        if (toon == null || toon.isBlank()) {
            return new LinkedHashMap<>();
        }

        // Special case: if input is exactly "null", return null
        final String trimmed = toon.trim();
        if (NULL_LITERAL.equals(trimmed)) {
            return null;
        }
        if ("[]".equals(trimmed)) {
            return java.util.Collections.emptyList();
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
            if (context.options.strict()) {
                final String key = line.substring(0, colonIdx).trim();
                // In strict mode, reject keys with unquoted brackets that didn't match
                // KEYED_ARRAY_PATTERN. This catches:
                //   - extra brackets between bracket segment and colon (foo[1][bar])
                //   - text between bracket segment and colon (foo[2]extra)
                //   - non-integer bracket segment (foo[bar])
                //   - negative bracket length (items[-1])
                //   - whitespace between bracket segment and colon/fields segment
                //     (items[2] :, items[2] {a,b}:)
                if (DecodeHelper.hasUnquotedBrackets(key)) {
                    throw new IllegalArgumentException(
                        "Invalid array header syntax at line " + (context.currentLine + 1));
                }
            }
            final String key = line.substring(0, colonIdx).trim();
            final String value = line.substring(colonIdx + 1).trim();
            return KeyDecoder.parseKeyValuePair(key, value, depth, depth == 0, context);
        }

        // Bare scalar value
        if (context.options.strict() && DecodeHelper.hasUnquotedBrackets(line)) {
            // Line has brackets but no colon and didn't match KEYED_ARRAY_PATTERN
            // (e.g. "items[2]{id,name}" missing colon)
            throw new IllegalArgumentException(
                "Invalid syntax: unquoted brackets without valid header at line "
                    + (context.currentLine + 1));
        }
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
            if (decoded == null) {
                return NULL_LITERAL;
            }
            return MAPPER.writeValueAsString(decoded);
        } catch (IllegalArgumentException e) {
            // decode() already threw, or strict-mode structural failure
            // re-throw with wrapping for consistency
            throw new IllegalArgumentException("Failed to convert decoded value to JSON", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert decoded value to JSON: " + e.getMessage(), e);
        }
    }
}
