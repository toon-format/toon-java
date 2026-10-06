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
     * Encodes any value as a list item. Arrays that are not all primitives
     * become a nested header with their items as list items one level deeper.
     *
     * @param value   The value to encode
     * @param writer  LineWriter for output
     * @param depth   Indentation depth of the "- " line
     * @param options Encoding options
     */
    static void encodeValueAsListItem(final JsonNode value,
                                      final LineWriter writer,
                                      final int depth,
                                      final EncodeOptions options) {
        final String delimiter = options.delimiter().toString();
        if (value.isValueNode()) {
            writer.push(depth, LIST_ITEM_PREFIX + PrimitiveEncoder.encodePrimitive(value, delimiter));
        } else if (value.isArray()) {
            final ArrayNode array = (ArrayNode) value;
            if (ArrayEncoder.isArrayOfPrimitives(array)) {
                writer.push(depth, LIST_ITEM_PREFIX + ArrayEncoder.formatInlineArray(array, delimiter, null));
                return;
            }
            writer.push(depth, LIST_ITEM_PREFIX + PrimitiveEncoder.formatHeader(array.size(), null, null, delimiter));
            for (JsonNode item : array) {
                encodeValueAsListItem(item, writer, depth + 1, options);
            }
        } else if (value.isObject()) {
            encodeObjectAsListItem((ObjectNode) value, writer, depth, options);
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
            encodeFirstValueAsArray(key, (ArrayNode) value, writer, depth, options);
        } else if (value.isObject()) {
            encodeFirstValueAsObject(key, encodedKey, (ObjectNode) value, writer, depth, options);
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
                                                final ArrayNode arrayValue,
                                                final LineWriter writer,
                                                final int depth,
                                                final EncodeOptions options) {
        if (ArrayEncoder.isArrayOfPrimitives(arrayValue)) {
            encodeFirstArrayAsPrimitives(key, arrayValue, writer, depth, options);
        } else if (ArrayEncoder.isArrayOfObjects(arrayValue)) {
            encodeFirstArrayAsObjects(key, arrayValue, writer, depth, options);
        } else {
            encodeFirstArrayAsComplex(key, arrayValue, writer, depth, options);
        }
    }

    private static void encodeFirstArrayAsPrimitives(final String key,
                                                       final ArrayNode arrayValue,
                                                       final LineWriter writer,
                                                       final int depth,
                                                       final EncodeOptions options) {
        final String formatted = ArrayEncoder.formatInlineArray(arrayValue, options.delimiter().toString(), key);
        writer.push(depth, LIST_ITEM_PREFIX + formatted);
    }

    private static void encodeFirstArrayAsObjects(final String key,
                                                    final ArrayNode arrayValue,
                                                    final LineWriter writer,
                                                    final int depth,
                                                    final EncodeOptions options) {
        final List<TabularField> header = TabularArrayEncoder.detectTabularHeader(arrayValue);
        if (!header.isEmpty()) {
            final String headerStr = PrimitiveEncoder.formatHeader(arrayValue.size(), key, header,
                                                                    options.delimiter().toString());
            writer.push(depth, LIST_ITEM_PREFIX + headerStr);
            // Write just the rows, header was already written above
            TabularArrayEncoder.writeTabularRows(arrayValue, header, writer, depth + 2, options);
        } else {
            writer.push(depth, LIST_ITEM_PREFIX
                + PrimitiveEncoder.formatHeader(arrayValue.size(), key, null, options.delimiter().toString()));
            for (JsonNode item : arrayValue) {
                if (item.isObject()) {
                    encodeObjectAsListItem((ObjectNode) item, writer, depth + 2, options);
                }
            }
        }
    }

    private static void encodeFirstArrayAsComplex(final String key,
                                                    final ArrayNode arrayValue,
                                                    final LineWriter writer,
                                                    final int depth,
                                                    final EncodeOptions options) {
        writer.push(depth, LIST_ITEM_PREFIX
            + PrimitiveEncoder.formatHeader(arrayValue.size(), key, null, options.delimiter().toString()));

        for (JsonNode item : arrayValue) {
            encodeValueAsListItem(item, writer, depth + 2, options);
        }
    }

    private static void encodeFirstValueAsObject(final String key,
                                                  final String encodedKey,
                                                  final ObjectNode nestedObj,
                                                  final LineWriter writer,
                                                  final int depth,
                                                  final EncodeOptions options) {
        final List<TabularField> keyedFields = KeyedObjectEncoder.detectKeyedFields(nestedObj);
        if (!keyedFields.isEmpty()) {
            final String headerStr = HeaderFormatter.formatKeyedHeader(nestedObj.size(), key, keyedFields,
                    options.delimiter().toString());
            writer.push(depth, LIST_ITEM_PREFIX + headerStr);
            KeyedObjectEncoder.writeKeyedRows(nestedObj, keyedFields, writer, depth + 2, options);
            return;
        }
        writer.push(depth, LIST_ITEM_PREFIX + encodedKey + COLON);
        if (!nestedObj.isEmpty()) {
            ObjectEncoder.encodeObject(nestedObj, writer, depth + 2, options, Set.of(), null, null, new HashSet<>());
        }
    }
}