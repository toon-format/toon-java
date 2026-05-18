package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.List;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_PREFIX;
import static dev.toonformat.jtoon.util.Constants.SPACE;

/**
 * Handles encoding of JSON arrays to TOON format.
 * Orchestrates array encoding by detecting array types and delegating to specialized encoders.
 */
public final class ArrayEncoder {

    private ArrayEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Main entry point for array encoding.
     * Detects array type and delegates to appropriate encoding method.
     *
     * @param key     Optional key prefix
     * @param value   ArrayNode to encode
     * @param writer  LineWriter for output
     * @param depth   Indentation depth
     * @param options Encoding options
     */
    public static void encodeArray(final String key, final ArrayNode value,
            final LineWriter writer, final int depth, final EncodeOptions options) {
        if (value.isEmpty()) {
            if (!options.lengthMarker()) {
                if (key == null && depth == 0) {
                    writer.push(depth, "[]");
                    return;
                }
                if (key != null) {
                    final String encodedKey = PrimitiveEncoder.encodeKey(key);
                    writer.push(depth, encodedKey + ": []");
                    return;
                }
            }
            final String header = PrimitiveEncoder.formatHeader(0, key, null, options.delimiter().toString(),
                    options.lengthMarker());
            writer.push(depth, header);
            return;
        }

        final int size = value.size();
        boolean allPrimitives = true;
        boolean allArrays = true;
        boolean allObjects = true;

        for (int i = 0; i < size; i++) {
            final JsonNode item = value.get(i);
            if (!item.isValueNode()) {
                allPrimitives = false;
            }
            if (!item.isArray()) {
                allArrays = false;
            }
            if (!item.isObject()) {
                allObjects = false;
            }
            if (!allPrimitives && !allArrays && !allObjects) {
                break;
            }
        }

        if (allPrimitives) {
            encodeInlinePrimitiveArray(key, value, writer, depth, options);
            return;
        }

        if (allArrays) {
            boolean allPrimitiveArrays = true;
            for (int i = 0; i < size; i++) {
                if (!isArrayOfPrimitives(value.get(i))) {
                    allPrimitiveArrays = false;
                    break;
                }
            }
            if (allPrimitiveArrays) {
                encodeArrayOfArraysAsListItems(key, value, writer, depth, options);
                return;
            }
        }

        if (allObjects) {
            final List<String> header = TabularArrayEncoder.detectTabularHeader(value);
            if (!header.isEmpty()) {
                TabularArrayEncoder.encodeArrayOfObjectsAsTabular(key, value, header, writer, depth, options);
            } else {
                encodeMixedArrayAsListItems(key, value, writer, depth, options);
            }
            return;
        }

        encodeMixedArrayAsListItems(key, value, writer, depth, options);
    }

    /**
     * Checks if an array contains only primitive values.
     *
     * @param array for testing that all items are primitives
     * @return true if all items in the array are primitive values, false otherwise
     */
    public static boolean isArrayOfPrimitives(final JsonNode array) {
        if (!array.isArray()) {
            return false;
        }
        for (JsonNode item : array) {
            if (!item.isValueNode()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if an array contains only arrays.
     *
     * @param array the array to check
     * @return true if all items in the array are arrays, false otherwise
     */
    static boolean isArrayOfArrays(final JsonNode array) {
        if (!array.isArray()) {
            return false;
        }
        for (JsonNode item : array) {
            if (!item.isArray()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if an array contains only objects.
     *
     * @param array the array to check
     * @return true if all items in the array are objects, false otherwise
     */
    public static boolean isArrayOfObjects(final JsonNode array) {
        if (!array.isArray()) {
            return false;
        }
        for (JsonNode item : array) {
            if (!item.isObject()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encodes a primitive array inline: key[N]: v1,v2,v3.
     */
    private static void encodeInlinePrimitiveArray(final String prefix, final ArrayNode values,
            final LineWriter writer, final int depth, final EncodeOptions options) {
        final String formatted = formatInlineArray(values, options.delimiter().toString(), prefix,
                options.lengthMarker());
        writer.push(depth, formatted);
    }

    /**
     * Formats an inline primitive array with header and values.
     *
     * @param values       the array of primitive values to format
     * @param delimiter    the delimiter to use between values
     * @param prefix       optional key prefix for the array
     * @param lengthMarker whether to include the # marker before the length
     * @return the formatted inline array string
     */
    public static String formatInlineArray(final ArrayNode values, final String delimiter,
            final String prefix, final boolean lengthMarker) {
        final String header = PrimitiveEncoder.formatHeader(values.size(), prefix, null, delimiter, lengthMarker);

        // Early return for empty arrays
        if (values.isEmpty()) {
            if (!lengthMarker && prefix != null) {
                return PrimitiveEncoder.encodeKey(prefix) + ": []";
            }
            return header;
        }

        // Build joined values directly without intermediate collection
        final StringBuilder joinedValues = new StringBuilder(128);
        boolean first = true;
        for (final JsonNode value : values) {
            if (!first) {
                joinedValues.append(delimiter);
            }
            first = false;
            joinedValues.append(PrimitiveEncoder.encodePrimitive(value, delimiter));
        }

        return header + SPACE + joinedValues;
    }

    /**
     * Encodes an array of primitive arrays as list items.
     */
    private static void encodeArrayOfArraysAsListItems(final String prefix, final ArrayNode values,
            final LineWriter writer, final int depth, final EncodeOptions options) {
        final String header = PrimitiveEncoder.formatHeader(values.size(), prefix, null,
                                                            options.delimiter().toString(), options.lengthMarker());
        writer.push(depth, header);

        for (JsonNode arr : values) {
            if (arr.isArray() && isArrayOfPrimitives(arr)) {
                final String inline = formatInlineArray((ArrayNode) arr, options.delimiter().toString(), null,
                                                        options.lengthMarker());
                writer.push(depth + 1, LIST_ITEM_PREFIX + inline);
            }
        }
    }

    /**
     * Encodes a mixed array (non-uniform) as list items.
     */
    private static void encodeMixedArrayAsListItems(final String prefix,
                                                    final ArrayNode items,
                                                    final LineWriter writer,
                                                    final int depth,
                                                    final EncodeOptions options) {
        final String header = PrimitiveEncoder.formatHeader(items.size(), prefix, null,
                                                            options.delimiter().toString(), options.lengthMarker());
        writer.push(depth, header);

        for (JsonNode item : items) {
            if (item.isValueNode()) {
                // Direct primitive as list item
                writer.push(depth + 1,
                        LIST_ITEM_PREFIX + PrimitiveEncoder.encodePrimitive(item, options.delimiter().toString()));
            } else if (item.isArray()) {
                // Direct array as list item
                if (isArrayOfPrimitives(item)) {
                    final String inline = formatInlineArray((ArrayNode) item, options.delimiter().toString(), null,
                                                            options.lengthMarker());
                    writer.push(depth + 1, LIST_ITEM_PREFIX + inline);
                }
                if (isArrayOfObjects(item)) {
                    final ArrayNode arrayItems = (ArrayNode) item;
                    final String nestedHeader = PrimitiveEncoder.formatHeader(arrayItems.size(), null, null,
                                                                              options.delimiter().toString(),
                                                                              options.lengthMarker());
                    writer.push(depth + 1, LIST_ITEM_PREFIX + nestedHeader);

                    arrayItems.elements().forEach(e -> ListItemEncoder.encodeObjectAsListItem((ObjectNode) e, writer,
                                                                                               depth + 2, options));
                }
            } else if (item.isObject()) {
                // Object as list item - delegate to ListItemEncoder
                ListItemEncoder.encodeObjectAsListItem((ObjectNode) item, writer, depth + 1, options);
            }
        }
    }
}
