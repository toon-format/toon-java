package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static dev.toonformat.jtoon.util.Constants.COLON;
import static dev.toonformat.jtoon.util.Constants.SPACE;

/**
 * Detects and encodes objects of uniform objects in keyed tabular form (§9.5).
 * The shared field structure is declared once in a keyed header; each entry
 * becomes one row carrying its own key.
 */
public final class KeyedObjectEncoder {

    private KeyedObjectEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Detects if an object can be encoded in keyed tabular form (§9.5):
     * at least two entries, every entry value a non-empty object with the same
     * key set, and every column uniform-primitive or nested-uniform (§9.3).
     * Returns the header fields (ordered by the first entry's encounter order)
     * or an empty list when the object must stay in nested form.
     *
     * @param entries the object whose values are candidate entry objects
     * @return keyed header fields, or empty list if not keyed-eligible
     */
    public static List<TabularField> detectKeyedFields(final ObjectNode entries) {
        if (entries.size() < 2) {
            return Collections.emptyList();
        }

        for (final JsonNode value : entries) {
            if (!value.isObject() || value.isEmpty()) {
                return Collections.emptyList();
            }
        }

        final String firstKey = entries.propertyNames().iterator().next();
        final ObjectNode firstEntry = (ObjectNode) entries.get(firstKey);
        final List<TabularField> header = new ArrayList<>();
        for (final String key : firstEntry.propertyNames()) {
            final Optional<List<TabularField>> children = TabularArrayEncoder.uniformColumnsOf(firstEntry.get(key));
            if (children.isEmpty()) {
                return Collections.emptyList();
            }
            header.add(new TabularField(key, children.get()));
        }

        if (!TabularArrayEncoder.matchesEveryRow(entries, header)) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(header);
    }

    /**
     * Encodes an object of uniform objects as a keyed table.
     *
     * @param prefix  optional key prefix (null for the root keyless form)
     * @param entries the object of uniform entry objects
     * @param fields  detected keyed header fields
     * @param writer  LineWriter for output
     * @param depth   Indentation depth
     * @param options Encoding options
     */
    public static void encodeKeyedTabularObject(@Nullable final String prefix, final ObjectNode entries,
            final List<TabularField> fields, final LineWriter writer, final int depth,
            final EncodeOptions options) {
        final String headerStr = HeaderFormatter.formatKeyedHeader(entries.size(), prefix, fields,
                options.delimiter().toString(), options.lengthMarker());
        writer.push(depth, headerStr);

        writeKeyedRows(entries, fields, writer, depth + 1, options);
    }

    /**
     * Writes entry rows: {@code entrykey: c1<delim>c2<delim>…} in entry
     * encounter order (§9.5). The entry key is encoded per §7.3 and followed
     * by a colon and a single space (§12).
     *
     * @param entries the object of uniform entry objects
     * @param fields  keyed header fields
     * @param writer  LineWriter for output
     * @param depth   Indentation depth
     * @param options Encoding options
     */
    public static void writeKeyedRows(final ObjectNode entries, final List<TabularField> fields,
            final LineWriter writer, final int depth, final EncodeOptions options) {
        final String delimiter = options.delimiter().toString();
        for (final String entryKey : entries.propertyNames()) {
            final ObjectNode entryValue = (ObjectNode) entries.get(entryKey);
            final List<String> cells = new ArrayList<>(fields.size());
            TabularArrayEncoder.collectCells(entryValue, fields, cells, delimiter);
            writer.push(depth,
                    PrimitiveEncoder.encodeKey(entryKey) + COLON + SPACE + String.join(delimiter, cells));
        }
    }
}
