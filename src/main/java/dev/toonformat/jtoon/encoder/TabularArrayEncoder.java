package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
     * Columns holding uniform nested objects are collapsed into nested field groups (§9.3).
     *
     * @param rows The array to analyze
     * @return List of header fields for tabular encoding, or empty list if not tabular
     */
    public static List<TabularField> detectTabularHeader(final ArrayNode rows) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        final JsonNode firstRow = rows.get(0);
        if (!firstRow.isObject()) {
            return Collections.emptyList();
        }

        final ObjectNode firstObj = (ObjectNode) firstRow;
        if (firstObj.isEmpty()) {
            return Collections.emptyList();
        }

        final List<TabularField> header = new ArrayList<>();
        for (final String key : firstObj.propertyNames()) {
            final Optional<List<TabularField>> children = uniformColumnsOf(firstObj.get(key));
            if (children.isEmpty()) {
                return Collections.emptyList();
            }
            header.add(new TabularField(key, children.get()));
        }

        if (!matchesEveryRow(rows, header)) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(header);
    }

    /**
     * Derives the nested field-group structure of a single column value.
     * Returns {@link Optional#empty()} when the column cannot be tabular (§9.3):
     * arrays, empty objects, and values whose structure differs per row.
     * A primitive (leaf) column yields an empty field list; a nested uniform
     * object column yields its child fields.
     */
    static Optional<List<TabularField>> uniformColumnsOf(final JsonNode value) {
        if (value.isValueNode()) {
            return Optional.of(Collections.emptyList());
        }
        if (!value.isObject()) {
            return Optional.empty();
        }
        final ObjectNode obj = (ObjectNode) value;
        if (obj.isEmpty()) {
            return Optional.empty();
        }
        final List<TabularField> children = new ArrayList<>();
        for (final String key : obj.propertyNames()) {
            final Optional<List<TabularField>> subChildren = uniformColumnsOf(obj.get(key));
            if (subChildren.isEmpty()) {
                return Optional.empty();
            }
            children.add(new TabularField(key, subChildren.get()));
        }
        return Optional.of(children);
    }

    /**
     * Checks that every row matches the header structure with uniform values.
     */
    static boolean matchesEveryRow(final Iterable<JsonNode> rows, final List<TabularField> header) {
        for (final JsonNode row : rows) {
            if (!row.isObject()) {
                return false;
            }

            final ObjectNode obj = (ObjectNode) row;
            if (obj.size() != header.size()) {
                return false;
            }

            for (final TabularField field : header) {
                if (!matchesField(field, obj.get(field.name()))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks that a row value matches the field structure: primitives for leaf
     * fields, uniformly structured objects for nested field groups.
     */
    private static boolean matchesField(final TabularField field, @Nullable final JsonNode value) {
        if (value == null) {
            return false;
        }
        if (field.isLeaf()) {
            return value.isValueNode();
        }
        if (!value.isObject()) {
            return false;
        }
        final ObjectNode obj = (ObjectNode) value;
        if (obj.size() != field.children().size()) {
            return false;
        }
        for (final TabularField child : field.children()) {
            if (!matchesField(child, obj.get(child.name()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encodes an array of objects as a tabular structure.
     *
     * @param prefix  Optional key prefix
     * @param rows    Array of uniform objects
     * @param header  List of header fields
     * @param writer  LineWriter for output
     * @param depth   Indentation depth
     * @param options Encoding options
     */
    public static void encodeArrayOfObjectsAsTabular(@Nullable final String prefix, final ArrayNode rows,
            final List<TabularField> header, final LineWriter writer, final int depth,
            final EncodeOptions options) {
        final String headerStr = PrimitiveEncoder.formatHeader(rows.size(), prefix, header,
                options.delimiter().toString(), options.lengthMarker());
        writer.push(depth, headerStr);

        writeTabularRows(rows, header, writer, depth + 1, options);
    }

    /**
     * Writes rows of tabular data by extracting leaf values in header order.
     * Public to allow ListItemEncoder to write rows after placing header on "- " line.
     *
     * @param rows    Array of objects
     * @param header  List of header fields
     * @param writer  LineWriter for output
     * @param depth   Indentation depth
     * @param options Encoding options
     */
    public static void writeTabularRows(final Iterable<JsonNode> rows, final List<TabularField> header,
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
     * Joins leaf values from a single row in depth-first pre-order (§9.3):
     * each nested field group contributes its own leaf cells before the next
     * sibling field. Missing keys are skipped.
     */
    private static String joinRowValues(final ObjectNode row, final List<TabularField> header, final String delimiter) {
        final List<String> cells = new ArrayList<>(header.size());
        collectCells(row, header, cells, delimiter);
        return String.join(delimiter, cells);
    }

    static void collectCells(final ObjectNode row, final List<TabularField> fields, final List<String> cells,
            final String delimiter) {
        for (final TabularField field : fields) {
            if (field.isLeaf()) {
                final JsonNode value = row.get(field.name());
                if (value != null) {
                    cells.add(PrimitiveEncoder.encodePrimitive(value, delimiter));
                }
                continue;
            }
            final JsonNode group = row.get(field.name());
            if (group != null && group.isObject()) {
                collectCells((ObjectNode) group, field.children(), cells, delimiter);
            }
        }
    }
}

