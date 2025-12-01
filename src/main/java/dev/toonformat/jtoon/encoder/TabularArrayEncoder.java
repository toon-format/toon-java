package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Detects and encodes uniform arrays of objects in efficient tabular format.
 * Tabular format declares field names once in a header and streams rows as CSV-like data.
 */
public final class TabularArrayEncoder {

    private TabularArrayEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Detects if an array can be encoded in tabular format.
     * Returns the header fields if tabular encoding is possible, empty list otherwise.
     *
     * @param rows The array to analyze
     * @return List of field names for tabular header, or empty list if not tabular
     */
    public static List<String> detectTabularHeader(ArrayNode rows) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        JsonNode firstRow = rows.get(0);
        if (!firstRow.isObject()) {
            return Collections.emptyList();
        }

        ObjectNode firstObj = (ObjectNode) firstRow;
        List<String> firstKeys = new ArrayList<>(firstObj.propertyNames());

        if (firstKeys.isEmpty()) {
            return Collections.emptyList();
        }

        if (isTabularArray(rows, firstKeys)) {
            return firstKeys;
        }

        return Collections.emptyList();
    }

    /**
     * Checks if all rows in the array have the same keys with primitive values.
     */
    private static boolean isTabularArray(ArrayNode rows, List<String> header) {
        for (JsonNode row : rows) {
            if (!row.isObject()) {
                return false;
            }

            ObjectNode obj = (ObjectNode) row;
            List<String> keys = new ArrayList<>(obj.propertyNames());

            // All objects must have the same keys (but order can differ)
            if (keys.size() != header.size()) {
                return false;
            }

            // Check that all header keys exist in the row and all values are primitives
            for (String key : header) {
                if (!obj.has(key)) {
                    return false;
                }
                if (!obj.get(key).isValueNode()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Encodes an array of objects as a tabular structure.
     *
     * @param prefix  Optional key prefix
     * @param rows    Array of uniform objects
     * @param header  List of field names
     * @param writer  LineWriter for output
     * @param depth   Indentation depth
     * @param options Encoding options
     */
    public static void encodeArrayOfObjectsAsTabular(String prefix, ArrayNode rows, List<String> header,
                                                     LineWriter writer, int depth, EncodeOptions options) {
        String headerStr = PrimitiveEncoder.formatHeader(rows.size(), prefix, header, options.delimiter().toString(),
                options.lengthMarker());
        writer.push(depth, headerStr);

        writeTabularRows(rows, header, writer, depth + 1, options);
    }

    /**
     * Writes rows of tabular data by extracting values in header order.
     * Public to allow ListItemEncoder to write rows after placing header on "- " line.
     *
     * @param rows    Array of objects
     * @param header  List of field names
     * @param writer  LineWriter for output
     * @param depth   Indentation depth
     * @param options Encoding options
     */
    public static void writeTabularRows(ArrayNode rows, List<String> header, LineWriter writer, int depth,
                                        EncodeOptions options) {
        for (JsonNode row : rows) {
            //skip non-object rows
            if (!row.isObject()) {
                continue;
            }
            ObjectNode obj = (ObjectNode) row;
            List<JsonNode> values = new ArrayList<>();
            for (String key : header) {
                values.add(obj.get(key));
            }
            String joinedValue = PrimitiveEncoder.joinEncodedValues(values, options.delimiter().toString());
            writer.push(depth, joinedValue);
        }
    }
}

