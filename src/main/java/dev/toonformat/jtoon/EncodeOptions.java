package dev.toonformat.jtoon;

/**
 * Configuration options for encoding data to JToon format.
 *
 * @param indent       Number of spaces per indentation level (default: 2)
 * @param delimiter    Delimiter to use for tabular array rows and inline
 *                     primitive arrays (default: COMMA)
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
     * Default encoding options: 2 spaces indent, comma delimiter, no length marker
     */
    public static final EncodeOptions DEFAULT = new EncodeOptions(2, Delimiter.COMMA, false, KeyFolding.OFF, Integer.MAX_VALUE);

    /**
     * Creates EncodeOptions with default values.
     */
    public EncodeOptions() {
        this(2, Delimiter.COMMA, false, KeyFolding.OFF, Integer.MAX_VALUE);
    }

    /**
     * Creates EncodeOptions with custom indent, using default delimiter and length
     * marker.
     *
     * @param indent number of spaces per indentation level
     * @return a new EncodeOptions instance with the specified indent
     */
    public static EncodeOptions withIndent(int indent) {
        return new EncodeOptions(indent, Delimiter.COMMA, false, KeyFolding.OFF, Integer.MAX_VALUE);
    }

    /**
     * Creates EncodeOptions with custom delimiter, using default indent and length
     * marker.
     *
     * @param delimiter the delimiter to use for tabular arrays and inline primitive arrays
     * @return a new EncodeOptions instance with the specified delimiter
     */
    public static EncodeOptions withDelimiter(Delimiter delimiter) {
        return new EncodeOptions(2, delimiter, false, KeyFolding.OFF, Integer.MAX_VALUE);
    }

    /**
     * Creates EncodeOptions with custom length marker, using default indent and
     * delimiter.
     *
     * @param lengthMarker whether to include the # marker before array lengths
     * @return a new EncodeOptions instance with the specified length marker setting
     */
    public static EncodeOptions withLengthMarker(boolean lengthMarker) {
        return new EncodeOptions(2, Delimiter.COMMA, lengthMarker, KeyFolding.OFF, Integer.MAX_VALUE);
    }

    /**
     * Creates EncodeOptions with custom flatten flag, using default indent and
     * delimiter.
     *
     * @param flatten optional flag to flatten nested objects to a single level.
     * @return a new EncodeOptions instance with the flatten setting
     */
    public static EncodeOptions withFlatten(boolean flatten) {
        return new EncodeOptions(2, Delimiter.COMMA, false, flatten ? KeyFolding.SAFE : KeyFolding.OFF, Integer.MAX_VALUE);
    }

    /**
     * Creates EncodeOptions with custom flatten flag and the depth of to flatten the nested objects, using default indent and
     * delimiter.
     *
     * @param flattenDepth optional maximum depth to flatten nested objects.
     * @return a new EncodeOptions instance with the flatten setting and the depth of to flatten the nested objects.
     */
    public static EncodeOptions withFlattenDepth(int flattenDepth) {
        return new EncodeOptions(2, Delimiter.COMMA, false, KeyFolding.SAFE, flattenDepth);
    }
}
