package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;

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

        DecodeParser parser = new DecodeParser(processed, options);
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
}
