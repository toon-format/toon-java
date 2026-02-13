package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_MARKER;
import static dev.toonformat.jtoon.util.Constants.COLON;
import static dev.toonformat.jtoon.util.Constants.SPACE;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_PREFIX;
import static dev.toonformat.jtoon.util.Constants.OPEN_BRACKET;
import static dev.toonformat.jtoon.util.Constants.CLOSE_BRACKET;

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
     * The first key-value appears on the "- " line, remaining fields are indented.
     *
     * @param obj     The object to encode
     * @param writer  LineWriter for output
     * @param depth   Indentation depth
     * @param options Encoding options
     */
    public static void encodeObjectAsListItem(final ObjectNode obj,
                                               final LineWriter writer,
                                               final int depth,
                                               final EncodeOptions options) {
        final List<String> keys = new ArrayList<>(obj.propertyNames());

        if (keys.isEmpty()) {
            writer.push(depth, LIST_ITEM_MARKER);
            return;
        }

        // First key-value on the same line as "- "
        final String firstKey = keys.get(0);
        final JsonNode firstValue = obj.get(firstKey);
        encodeFirstKeyValue(firstKey, firstValue, writer, depth, options);

        // Remaining keys on indented lines
        for (int i = 1; i < keys.size(); i++) {
            final String key = keys.get(i);
            ObjectEncoder.encodeKeyValuePair(key, obj.get(key), writer, depth + 1, options, new HashSet<>(keys),
                                             Set.of(), null, null, new HashSet<>());
        }
    }

    /**
     * Encodes the first key-value pair of a list item.
     * Handles special formatting for arrays and objects.
     */
    private static void encodeFirstKeyValue(final String key,
                                             final JsonNode value,
                                             final LineWriter writer,
                                             final int depth,
                                             final EncodeOptions options) {
        final String encodedKey = PrimitiveEncoder.encodeKey(key);

        if (value.isValueNode()) {
            encodeFirstValueAsPrimitive(encodedKey, value, writer, depth, options);
        } else if (value.isArray()) {
            encodeFirstValueAsArray(key, encodedKey, (ArrayNode) value, writer, depth, options);
        } else if (value.isObject()) {
            encodeFirstValueAsObject(encodedKey, (ObjectNode) value, writer, depth, options);
        }
    }

    private static void encodeFirstValueAsPrimitive(final String encodedKey,
                                                     final JsonNode value,
                                                     final LineWriter writer,
                                                     final int depth,
                                                     final EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + COLON + SPACE
                + PrimitiveEncoder.encodePrimitive(value, options.delimiter().toString()));
    }

    private static void encodeFirstValueAsArray(final String key,
                                                final String encodedKey,
                                                final ArrayNode arrayValue,
                                                final LineWriter writer,
                                                final int depth,
                                                final EncodeOptions options) {
        if (ArrayEncoder.isArrayOfPrimitives(arrayValue)) {
            encodeFirstArrayAsPrimitives(key, arrayValue, writer, depth, options);
        } else if (ArrayEncoder.isArrayOfObjects(arrayValue)) {
            encodeFirstArrayAsObjects(key, encodedKey, arrayValue, writer, depth, options);
        } else {
            encodeFirstArrayAsComplex(encodedKey, arrayValue, writer, depth, options);
        }
    }

    private static void encodeFirstArrayAsPrimitives(final String key,
                                                     final ArrayNode arrayValue,
                                                     final LineWriter writer,
                                                     final int depth,
                                                     final EncodeOptions options) {
        final String formatted = ArrayEncoder.formatInlineArray(arrayValue, options.delimiter().toString(), key,
                                                                options.lengthMarker());
        writer.push(depth, LIST_ITEM_PREFIX + formatted);
    }

    private static void encodeFirstArrayAsObjects(final String key,
                                                  final String encodedKey,
                                                  final ArrayNode arrayValue,
                                                  final LineWriter writer,
                                                  final int depth,
                                                  final EncodeOptions options) {
        final List<String> header = TabularArrayEncoder.detectTabularHeader(arrayValue);
        if (!header.isEmpty()) {
            final String headerStr = PrimitiveEncoder.formatHeader(arrayValue.size(), key, header,
                                                                   options.delimiter().toString(),
                                                                   options.lengthMarker());
            writer.push(depth, LIST_ITEM_PREFIX + headerStr);
            // Write just the rows, header was already written above
            TabularArrayEncoder.writeTabularRows(arrayValue, header, writer, depth + 2, options);
        } else {
            writer.push(depth,
                    LIST_ITEM_PREFIX + encodedKey + OPEN_BRACKET + arrayValue.size() + CLOSE_BRACKET + COLON);
            for (JsonNode item : arrayValue) {
                if (item.isObject()) {
                    encodeObjectAsListItem((ObjectNode) item, writer, depth + 2, options);
                }
            }
        }
    }

    private static void encodeFirstArrayAsComplex(final String encodedKey,
                                                  final ArrayNode arrayValue,
                                                  final LineWriter writer,
                                                  final int depth,
                                                  final EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + OPEN_BRACKET + arrayValue.size() + CLOSE_BRACKET + COLON);

        for (JsonNode item : arrayValue) {
            if (item.isValueNode()) {
                writer.push(depth + 2, LIST_ITEM_PREFIX
                        + PrimitiveEncoder.encodePrimitive(item, options.delimiter().toString()));
            } else if (item.isArray() && ArrayEncoder.isArrayOfPrimitives(item)) {
                final String inline = ArrayEncoder.formatInlineArray((ArrayNode) item, options.delimiter().toString(),
                                                                     null, options.lengthMarker());
                writer.push(depth + 2, LIST_ITEM_PREFIX + inline);
            } else if (item.isObject()) {
                encodeObjectAsListItem((ObjectNode) item, writer, depth + 2, options);
            }
        }
    }

    private static void encodeFirstValueAsObject(final String encodedKey,
                                                final ObjectNode nestedObj,
                                                final LineWriter writer,
                                                final int depth,
                                                final EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + COLON);
        if (!nestedObj.isEmpty()) {
            ObjectEncoder.encodeObject(nestedObj, writer, depth + 2, options, Set.of(), null, null, new HashSet<>());
        }
    }
}

