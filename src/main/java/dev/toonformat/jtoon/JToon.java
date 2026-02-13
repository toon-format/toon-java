package dev.toonformat.jtoon;

import dev.toonformat.jtoon.decoder.ValueDecoder;
import dev.toonformat.jtoon.encoder.ValueEncoder;
import dev.toonformat.jtoon.normalizer.JsonNormalizer;
import tools.jackson.databind.JsonNode;

/**
 * Main entry point for encoding and decoding TOON (Token-Oriented Object Notation) format.
 * Provides static methods to convert between Java objects, JSON strings, and TOON format.
 */
public final class JToon {

    private JToon() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encodes a Java object to JToon format using default options.
     * 
     * <p>
     * The object is first normalized (Java types are converted to JSON-compatible
     * representations), then encoded to JToon format.
     * </p>
     * 
     * @param input The object to encode (can be null)
     * @return The JToon-formatted string
     */
    public static String encode(final Object input) {
        return encode(input, EncodeOptions.DEFAULT);
    }

    /**
     * Encodes a Java object to JToon format using custom options.
     * 
     * <p>
     * The object is first normalized (Java types are converted to JSON-compatible
     * representations), then encoded to JToon format.
     * </p>
     * 
     * @param input   The object to encode (can be null)
     * @param options Encoding options (indent, delimiter, length marker)
     * @return The JToon-formatted string
     */
    public static String encode(final Object input, final EncodeOptions options) {
        final JsonNode normalizedValue = JsonNormalizer.normalize(input);
        return ValueEncoder.encodeValue(normalizedValue, options);
    }

    /**
     * Encodes a plain JSON string to TOON format using default options.
     *
     * <p>
     * This is a convenience overload that parses the JSON string and encodes it
     * without requiring callers to create a {@code JsonNode} or intermediate
     * objects.
     * </p>
     *
     * @param json The JSON string to encode (must be valid JSON)
     * @return The TOON-formatted string
     * @throws IllegalArgumentException if the input is not valid JSON
     */
    public static String encodeJson(final String json) {
        return encodeJson(json, EncodeOptions.DEFAULT);
    }

    /**
     * Encodes a plain JSON string to TOON format using custom options.
     *
     * <p>
     * Parsing is delegated to
     * {@link JsonNormalizer#parse(String)}
     * to maintain separation of concerns.
     * </p>
     *
     * @param json    The JSON string to encode (must be valid JSON)
     * @param options Encoding options (indent, delimiter, length marker)
     * @return The TOON-formatted string
     * @throws IllegalArgumentException if the input is not valid JSON
     */
    public static String encodeJson(final String json, final EncodeOptions options) {
        final JsonNode parsed = JsonNormalizer.parse(json);
        return ValueEncoder.encodeValue(parsed, options);
    }

    /**
     * Decodes a TOON-formatted string to Java objects using default options.
     *
     * <p>
     * Returns a Map for objects, List for arrays, or primitives (String, Number,
     * Boolean, null) for scalar values.
     * </p>
     *
     * @param toon The TOON-formatted string to decode
     * @return Parsed object (Map, List, primitive, or null)
     * @throws IllegalArgumentException if strict mode is enabled and input is
     *                                  invalid
     */
    public static Object decode(final String toon) {
        return decode(toon, DecodeOptions.DEFAULT);
    }

    /**
     * Decodes a TOON-formatted string to Java objects using custom options.
     *
     * <p>
     * Returns a Map for objects, List for arrays, or primitives (String, Number,
     * Boolean, null) for scalar values.
     * </p>
     *
     * @param toon    The TOON-formatted string to decode
     * @param options Decoding options (indent, delimiter, strict mode)
     * @return Parsed object (Map, List, primitive, or null)
     * @throws IllegalArgumentException if strict mode is enabled and input is
     *                                  invalid
     */
    public static Object decode(final String toon, final DecodeOptions options) {
        return ValueDecoder.decode(toon, options);
    }

    /**
     * Decodes a TOON-formatted string directly to a JSON string using default
     * options.
     *
     * <p>
     * This is a convenience method that decodes TOON to Java objects and then
     * serializes them to JSON.
     * </p>
     *
     * @param toon The TOON-formatted string to decode
     * @return JSON string representation
     * @throws IllegalArgumentException if strict mode is enabled and input is
     *                                  invalid
     */
    public static String decodeToJson(final String toon) {
        return decodeToJson(toon, DecodeOptions.DEFAULT);
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
        return ValueDecoder.decodeToJson(toon, options);
    }
}
