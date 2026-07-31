package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Core encoding orchestrator for converting JsonNode values to TOON format.
 * Delegates to specialized encoders based on node type.
 */
public final class ValueEncoder {

    private ValueEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encodes a normalized JsonNode value to TOON format.
     * 
     * @param value   The JsonNode to encode (can be null)
     * @param options Encoding options (indent, delimiter, length marker)
     * @return The TOON-formatted string
     */
    public static String encodeValue(final JsonNode value, final EncodeOptions options) {
        // Handle null values
        if (value == null || value.isNull()) {
            return "null";
        }

        // Handle primitive values directly
        if (value.isValueNode()) {
            return PrimitiveEncoder.encodePrimitive(value, options.delimiter().toString());
        }

        // Complex values need a LineWriter for indentation
        final LineWriter writer = new LineWriter(options.indent());

        if (value.isArray()) {
            ArrayEncoder.encodeArray(null, (ArrayNode) value, writer, 0, options);
        } else if (value.isObject()) {
            final ObjectNode obj = (ObjectNode) value;
            final List<TabularField> keyedFields = KeyedObjectEncoder.detectKeyedFields(obj);
            if (!keyedFields.isEmpty()) {
                KeyedObjectEncoder.encodeKeyedTabularObject(null, obj, keyedFields, writer, 0, options);
            } else {
                final Set<String> jsonNodes = new HashSet<>(value.propertyNames());
                ObjectEncoder.encodeObject(obj, writer, 0, options, jsonNodes, null, null, new HashSet<>());
            }
        }

        return writer.toString();
    }
}
