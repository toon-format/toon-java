package dev.toonformat.jtoon;

/**
 * Configuration options for decoding TOON format to Java objects.
 *
 * @param indent       Number of spaces per indentation level (default: 2)
 * @param delimiter    Delimiter expected in tabular array rows and inline
 *                     primitive arrays (default: COMMA)
 * @param strict       Strict validation mode. When true, throws
 *                     IllegalArgumentException on invalid input. When false,
 *                     uses best-effort parsing and returns null on errors
 *                     (default: true)
 * @param expandPaths  Path expansion mode for dotted keys (default: OFF)
 */
public record DecodeOptions(
        int indent,
        Delimiter delimiter,
        boolean strict,
        PathExpansion expandPaths) {
    /**
     * Default decoding options: 2 spaces indent, comma delimiter, strict validation, path expansion off.
     */
    public static final DecodeOptions DEFAULT = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);

    /**
     * Creates DecodeOptions with default values.
     */
    public DecodeOptions() {
        this(2, Delimiter.COMMA, true, PathExpansion.OFF);
    }

    /**
     * Creates DecodeOptions with custom indent, using default delimiter and strict
     * mode.
     * @param indent number of spaces per indentation level
     * @return a new DecodeOptions instance with the specified indent
     */
    public static DecodeOptions withIndent(final int indent) {
        return new DecodeOptions(indent, Delimiter.COMMA, true, PathExpansion.OFF);
    }

    /**
     * Creates DecodeOptions with custom delimiter, using default indent and strict
     * mode.
     * @param delimiter the delimiter to use for tabular arrays and inline primitive arrays
     * @return a new DecodeOptions instance with the specified delimiter
     */
    public static DecodeOptions withDelimiter(final Delimiter delimiter) {
        return new DecodeOptions(2, delimiter, true, PathExpansion.OFF);
    }

    /**
     * Creates DecodeOptions with custom strict mode, using default indent and
     * delimiter.
     * @param strict whether to enable strict validation mode
     * @return a new DecodeOptions instance with the specified strict mode
     */
    public static DecodeOptions withStrict(final boolean strict) {
        return new DecodeOptions(2, Delimiter.COMMA, strict, PathExpansion.OFF);
    }
}
