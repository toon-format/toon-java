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
    public static List<String> detectTabularHeader(final ArrayNode rows) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        final JsonNode firstRow = rows.get(0);
        if (!firstRow.isObject()) {
            return Collections.emptyList();
        }

        final ObjectNode firstObj = (ObjectNode) firstRow;
        final List<String> firstKeys = new ArrayList<>(firstObj.propertyNames());

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
    private static boolean isTabularArray(final Iterable<JsonNode> rows, final Iterable<String> header) {
        final List<String> headerList = new ArrayList<>();
        for (String h : header) {
            headerList.add(h);
        }

        for (JsonNode row : rows) {
            if (!row.isObject()) {
                return false;
            }

            final ObjectNode obj = (ObjectNode) row;
            final List<String> keys = new ArrayList<>(obj.propertyNames());

            // All objects must have the same keys (but order can differ)
            if (keys.size() != headerList.size()) {
                return false;
            }

            // Check that all header keys exist in the row and all values are primitives
            for (String key : headerList) {
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
    public static void encodeArrayOfObjectsAsTabular(final String prefix, final ArrayNode rows,
            final List<String> header, final LineWriter writer, final int depth,
            final EncodeOptions options) {
        final String headerStr = PrimitiveEncoder.formatHeader(rows.size(), prefix, header,
                options.delimiter().toString(), options.lengthMarker());
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
    public static void writeTabularRows(final Iterable<JsonNode> rows, final Iterable<String> header,
            final LineWriter writer, final int depth, final EncodeOptions options) {
        final List<String> headerList = new ArrayList<>();
        for (String h : header) {
            headerList.add(h);
        }
        final int headerSize = headerList.size();

        for (JsonNode row : rows) {
            //skip non-object rows
            if (!row.isObject()) {
                continue;
            }
            final ObjectNode obj = (ObjectNode) row;
            final List<JsonNode> values = new ArrayList<>(headerSize);
            for (String key : headerList) {
                values.add(obj.get(key));
            }
            final String joinedValue = PrimitiveEncoder.joinEncodedValues(values, options.delimiter().toString());
            writer.push(depth, joinedValue);
        }
    }
}

