package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import org.jspecify.annotations.Nullable;
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
    private static boolean isTabularArray(final Iterable<JsonNode> rows, final List<String> header) {
        final int headerSize = header.size();

        for (JsonNode row : rows) {
            if (!row.isObject()) {
                return false;
            }

            final ObjectNode obj = (ObjectNode) row;

            // All objects must have the same number of keys
            if (obj.size() != headerSize) {
                return false;
            }

            // Check that all header keys exist in the row and all values are primitives
            for (final String key : header) {
                final JsonNode value = obj.get(key);
                if (value == null || !value.isValueNode()) {
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
    public static void encodeArrayOfObjectsAsTabular(@Nullable final String prefix, final ArrayNode rows,
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
    public static void writeTabularRows(final Iterable<JsonNode> rows, final List<String> header,
            final LineWriter writer, final int depth, final EncodeOptions options) {
        for (JsonNode row : rows) {
            // Skip non-object rows
            if (!row.isObject()) {
                continue;
            }
            final ObjectNode obj = (ObjectNode) row;
            final String joinedValue = joinRowValues(obj, header, options.delimiter().toString());
            writer.push(depth, joinedValue);
        }
    }

    /**
     * Joins values from a single row according to header order.
     * Avoids creating intermediate collections.
     * Missing keys are skipped.
     */
    private static String joinRowValues(final ObjectNode row, final List<String> header, final String delimiter) {
        final StringBuilder sb = new StringBuilder(128);
        boolean first = true;
        for (final String key : header) {
            final JsonNode value = row.get(key);
            if (value == null) {
                continue; // Skip missing keys
            }
            if (!first) {
                sb.append(delimiter);
            }
            first = false;
            sb.append(PrimitiveEncoder.encodePrimitive(value, delimiter));
        }
        return sb.toString();
    }
}

