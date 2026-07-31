package dev.toonformat.jtoon.encoder;

import java.util.List;

/**
 * A field of a tabular header. Leaf fields hold primitive values encoded as a single
 * cell; non-leaf fields represent nested uniform object columns collapsed into nested
 * field groups (§9.3 of the TOON specification).
 *
 * @param name     Field name
 * @param children Child fields of the nested group; empty for a leaf field
 */
public record TabularField(String name, List<TabularField> children) {

    /**
     * Creates a leaf field whose values are encoded as a single tabular cell.
     *
     * @param name Field name
     * @return Leaf field with no children
     */
    public static TabularField leaf(final String name) {
        return new TabularField(name, List.of());
    }

    /**
     * Checks whether this field is a leaf (single-cell primitive column).
     *
     * @return true if the field has no child fields
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }
}
