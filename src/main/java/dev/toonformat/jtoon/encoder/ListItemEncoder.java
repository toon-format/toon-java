package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.toonformat.jtoon.util.Constants.*;

/**
 * Handles encoding of objects as list items in non-uniform arrays.
 * Implements the complex logic for placing the first field on the "- " line
 * and indenting remaining fields.
 */
public final class ListItemEncoder {

    private ListItemEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encodes an object as a list item.
     * First key-value appears on the "- " line, remaining fields are indented.
     *
     * @param obj     The object to encode
     * @param writer  LineWriter for output
     * @param depth   Indentation depth
     * @param options Encoding options
     */
    public static void encodeObjectAsListItem(ObjectNode obj, LineWriter writer, int depth, EncodeOptions options) {
        List<String> keys = new ArrayList<>(obj.propertyNames());

        if (keys.isEmpty()) {
            writer.push(depth, LIST_ITEM_MARKER);
            return;
        }

        // First key-value on the same line as "- "
        String firstKey = keys.get(0);
        JsonNode firstValue = obj.get(firstKey);
        encodeFirstKeyValue(firstKey, firstValue, writer, depth, options);

        // Remaining keys on indented lines
        for (int i = 1; i < keys.size(); i++) {
            String key = keys.get(i);
            ObjectEncoder.encodeKeyValuePair(key, obj.get(key), writer, depth + 1, options, new HashSet<>(keys), Set.of(), null, null, new HashSet<>());
        }
    }

    /**
     * Encodes the first key-value pair of a list item.
     * Handles special formatting for arrays and objects.
     */
    private static void encodeFirstKeyValue(String key, JsonNode value, LineWriter writer, int depth,
                                            EncodeOptions options) {
        String encodedKey = PrimitiveEncoder.encodeKey(key);

        if (value.isValueNode()) {
            encodeFirstValueAsPrimitive(encodedKey, value, writer, depth, options);
        } else if (value.isArray()) {
            encodeFirstValueAsArray(key, encodedKey, (ArrayNode) value, writer, depth, options);
        } else if (value.isObject()) {
            encodeFirstValueAsObject(encodedKey, (ObjectNode) value, writer, depth, options);
        }
    }

    private static void encodeFirstValueAsPrimitive(String encodedKey, JsonNode value, LineWriter writer, int depth,
                                                    EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + COLON + SPACE
                + PrimitiveEncoder.encodePrimitive(value, options.delimiter().getValue()));
    }

    private static void encodeFirstValueAsArray(String key, String encodedKey, ArrayNode arrayValue, LineWriter writer,
                                                int depth, EncodeOptions options) {
        if (ArrayEncoder.isArrayOfPrimitives(arrayValue)) {
            encodeFirstArrayAsPrimitives(key, arrayValue, writer, depth, options);
        } else if (ArrayEncoder.isArrayOfObjects(arrayValue)) {
            encodeFirstArrayAsObjects(key, encodedKey, arrayValue, writer, depth, options);
        } else {
            encodeFirstArrayAsComplex(encodedKey, arrayValue, writer, depth, options);
        }
    }

    private static void encodeFirstArrayAsPrimitives(String key, ArrayNode arrayValue, LineWriter writer, int depth,
                                                     EncodeOptions options) {
        String formatted = ArrayEncoder.formatInlineArray(arrayValue, options.delimiter().getValue(), key,
                options.lengthMarker());
        writer.push(depth, LIST_ITEM_PREFIX + formatted);
    }

    private static void encodeFirstArrayAsObjects(String key, String encodedKey, ArrayNode arrayValue,
                                                  LineWriter writer, int depth, EncodeOptions options) {
        List<String> header = TabularArrayEncoder.detectTabularHeader(arrayValue);
        if (!header.isEmpty()) {
            String headerStr = PrimitiveEncoder.formatHeader(arrayValue.size(), key, header,
                    options.delimiter().getValue(), options.lengthMarker());
            writer.push(depth, LIST_ITEM_PREFIX + headerStr);
            // Write just the rows, header was already written above
            TabularArrayEncoder.writeTabularRows(arrayValue, header, writer, depth + 1, options);
        } else {
            writer.push(depth,
                    LIST_ITEM_PREFIX + encodedKey + OPEN_BRACKET + arrayValue.size() + CLOSE_BRACKET + COLON);
            for (JsonNode item : arrayValue) {
                if (item.isObject()) {
                    encodeObjectAsListItem((ObjectNode) item, writer, depth + 1, options);
                }
            }
        }
    }

    private static void encodeFirstArrayAsComplex(String encodedKey, ArrayNode arrayValue, LineWriter writer, int depth,
                                                  EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + OPEN_BRACKET + arrayValue.size() + CLOSE_BRACKET + COLON);

        for (JsonNode item : arrayValue) {
            if (item.isValueNode()) {
                writer.push(depth + 1, LIST_ITEM_PREFIX
                        + PrimitiveEncoder.encodePrimitive(item, options.delimiter().getValue()));
            } else if (item.isArray() && ArrayEncoder.isArrayOfPrimitives(item)) {
                String inline = ArrayEncoder.formatInlineArray((ArrayNode) item, options.delimiter().getValue(), null,
                        options.lengthMarker());
                writer.push(depth + 1, LIST_ITEM_PREFIX + inline);
            } else if (item.isObject()) {
                encodeObjectAsListItem((ObjectNode) item, writer, depth + 1, options);
            }
        }
    }

    private static void encodeFirstValueAsObject(String encodedKey, ObjectNode nestedObj, LineWriter writer, int depth,
                                                 EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + COLON);
        if (!nestedObj.isEmpty()) {
            ObjectEncoder.encodeObject(nestedObj, writer, depth + 2, options, Set.of(), null, null, new HashSet<>());
        }
    }
}

