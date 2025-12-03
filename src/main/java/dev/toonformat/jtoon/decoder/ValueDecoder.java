package dev.toonformat.jtoon.decoder;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.toonformat.jtoon.DecodeOptions;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.afterburner.AfterburnerModule;

import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.regex.Matcher;

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

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = JsonMapper.builder()
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.ALWAYS))
            .addModule(new AfterburnerModule().setUseValueClassLoader(true)) // Speeds up Jackson by 20–40% in most real-world cases
            // .disable(MapperFeature.DEFAULT_VIEW_INCLUSION) in Jackson 3 this is default disabled
            // .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) in Jackson 3 this is default disabled
            .defaultTimeZone(TimeZone.getTimeZone("UTC")) // set a default timezone for dates
            .build();
    }

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
        String processed = Character.isWhitespace(toon.charAt(toon.length() - 1)) ? toon.stripTrailing() : toon;

        //set an own decode context
        final DecodeContext context = new DecodeContext();
        context.lines = processed.split("\r?\n", -1);
        context.options = options;
        context.delimiter = options.delimiter().toString();

        int lineIndex = context.currentLine;
        String line = context.lines[lineIndex];
        int depth = DecodeHelper.getDepth(line, context);

        if (depth > 0) {
            if (context.options.strict()) {
                throw new IllegalArgumentException("Unexpected indentation at line " + lineIndex);
            }
            return new LinkedHashMap<>();
        }

        String content = depth == 0 ? line : line.substring(depth * context.options.indent());

        // Handle standalone arrays: [2]:
        if (!content.isEmpty() && content.charAt(0) == '[') {
            return ArrayDecoder.parseArray(content, depth, context);
        }

        // Handle keyed arrays: items[2]{id,name}:
        Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
        if (keyedArray.matches()) {
            return KeyDecoder.parseKeyedArrayValue(keyedArray, content, depth, context);
        }
        // Handle key-value pairs: name: Ada
        int colonIdx = DecodeHelper.findUnquotedColon(content);
        if (colonIdx > 0) {
            String key = content.substring(0, colonIdx).trim();
            String value = content.substring(colonIdx + 1).trim();
            return KeyDecoder.parseKeyValuePair(key, value, depth, depth == 0, context);
        }

        // Bare scalar value
        return ObjectDecoder.parseBareScalarValue(content, depth, context);
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
            Object decoded = decode(toon, options);
            return OBJECT_MAPPER.writeValueAsString(decoded);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert decoded value to JSON: " + e.getMessage(), e);
        }
    }
}
