package dev.toonformat.jtoon;

import java.util.Objects;

/**
 * Configuration options for encoding data to JToon format.
 *
 * @param indent       Number of spaces per indentation level (default: 2)
 * @param delimiter    Delimiter used for both document delimiter and active
 *                     array delimiter. Controls quoting for object field values
 *                     (document delimiter) and inline array values / tabular
 *                     rows (active delimiter). (default: COMMA)
 * @param lengthMarker Optional marker to prefix array lengths in headers. When
 *                     true, arrays render as [#N] instead of [N] (default:
 *                     false)
 * @param flatten      Key folding mode n nested objects to a single level.
 *                     (default: OFF)
 * @param flattenDepth Optional maximum depth to flatten nested objects.
 *                     (default: Integer.MAX_VALUE)
 */
public record EncodeOptions(
        int indent,
        Delimiter delimiter,
        boolean lengthMarker,
        KeyFolding flatten,
        int flattenDepth) {
    /**
     * Default encoding options: 2 spaces indent, comma delimiter, no length marker.
     */
    public static final EncodeOptions DEFAULT = new EncodeOptions(
            2, Delimiter.COMMA, false, KeyFolding.OFF, Integer.MAX_VALUE);

    /**
     * Maximum allowed indent to prevent memory exhaustion attacks.
     */
    public static final int MAX_ALLOWED_INDENT = 100;

    /**
     * Creates EncodeOptions with default values.
     */
    public EncodeOptions() {
        this(2, Delimiter.COMMA, false, KeyFolding.OFF, Integer.MAX_VALUE);
    }

    /**
     * Compact constructor with validation.
     *
     * @param indent        number of spaces per indentation level
     * @param delimiter     delimiter for tabular array rows and inline arrays
     * @param lengthMarker  whether to prefix array lengths with {@code #}
     * @param flatten       key folding mode for nested objects
     * @param flattenDepth  maximum depth of key folding
     */
    public EncodeOptions {
        if (indent < 0) {
            throw new IllegalArgumentException("indent must be non-negative, got: " + indent);
        }
        if (indent > MAX_ALLOWED_INDENT) {
            throw new IllegalArgumentException("indent must be <= " + MAX_ALLOWED_INDENT + ", got: " + indent);
        }
        Objects.requireNonNull(delimiter, "delimiter cannot be null");
        if (flattenDepth < 0) {
            throw new IllegalArgumentException("flattenDepth must be non-negative, got: " + flattenDepth);
        }
    }

    /**
     * Creates EncodeOptions with custom indent, using default delimiter and length
     * marker.
     *
     * @param indent number of spaces per indentation level
     * @return a new EncodeOptions instance with the specified indent
     */
    public static EncodeOptions withIndent(final int indent) {
        return new EncodeOptions(indent, Delimiter.COMMA, false, KeyFolding.OFF, Integer.MAX_VALUE);
    }

    /**
     * Creates EncodeOptions with custom delimiter, using default indent and length
     * marker.
     *
     * @param delimiter the delimiter to use for tabular arrays and inline primitive arrays
     * @return a new EncodeOptions instance with the specified delimiter
     */
    public static EncodeOptions withDelimiter(final Delimiter delimiter) {
        return new EncodeOptions(2, delimiter, false, KeyFolding.OFF, Integer.MAX_VALUE);
    }

    /**
     * Creates EncodeOptions with custom length marker, using default indent and
     * delimiter.
     *
     * @param lengthMarker whether to include the # marker before array lengths
     * @return a new EncodeOptions instance with the specified length marker setting
     */
    public static EncodeOptions withLengthMarker(final boolean lengthMarker) {
        return new EncodeOptions(2, Delimiter.COMMA, lengthMarker, KeyFolding.OFF, Integer.MAX_VALUE);
    }

    /**
     * Creates EncodeOptions with custom flatten flag, using default indent and
     * delimiter.
     *
     * @param flatten optional flag to flatten nested objects to a single level.
     * @return a new EncodeOptions instance with the flatten setting
     */
    public static EncodeOptions withFlatten(final boolean flatten) {
        return new EncodeOptions(2, Delimiter.COMMA, false,
                flatten ? KeyFolding.SAFE : KeyFolding.OFF, Integer.MAX_VALUE);
    }

    /**
     * Creates EncodeOptions with custom flatten flag and the depth of to flatten
     * the nested objects, using default indent and delimiter.
     *
     * @param flattenDepth optional maximum depth to flatten nested objects.
     * @return a new EncodeOptions instance with the flatten setting and the depth of to flatten the nested objects.
     */
    public static EncodeOptions withFlattenDepth(final int flattenDepth) {
        return new EncodeOptions(2, Delimiter.COMMA, false, KeyFolding.SAFE, flattenDepth);
    }
}
