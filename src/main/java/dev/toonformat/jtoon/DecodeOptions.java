package dev.toonformat.jtoon;

import java.util.Objects;

/**
 * Configuration options for decoding TOON format to Java objects.
 *
 * @param indent          Number of spaces per indentation level (default: 2)
 * @param delimiter       Delimiter expected in tabular array rows and inline
 *                        primitive arrays (default: COMMA)
 * @param strict          Strict validation mode. When true, throws
 *                        IllegalArgumentException on invalid input. When false,
 *                        uses best-effort parsing and returns null on errors
 *                        (default: true)
 * @param expandPaths     Path expansion mode for dotted keys (default: OFF)
 * @param maxDepth        Maximum allowed nesting depth during decoding (default: 512).
 *                        Prevents StackOverflowError from deeply nested input.
 * @param maxArraySize    Maximum allowed elements in a single array (default: 10,000,000).
 *                        Prevents memory exhaustion from oversized arrays.
 * @param maxStringLength Maximum allowed length for string values (default: 10,000,000).
 *                        Prevents memory exhaustion from oversized strings.
 */
public record DecodeOptions(
        int indent,
        Delimiter delimiter,
        boolean strict,
        PathExpansion expandPaths,
        int maxDepth,
        int maxArraySize,
        int maxStringLength) {
    /**
     * Maximum allowed indent to prevent memory exhaustion attacks.
     */
    public static final int MAX_ALLOWED_INDENT = 100;

    /**
     * Maximum allowed nesting depth during decoding.
     */
    public static final int MAX_ALLOWED_DEPTH = 512;

    /**
     * Default maximum array size when not explicitly specified.
     */
    public static final int DEFAULT_MAX_ARRAY_SIZE = 10_000_000;

    /**
     * Default maximum string length when not explicitly specified.
     */
    public static final int DEFAULT_MAX_STRING_LENGTH = 10_000_000;

    /**
     * Default decoding options: 2 spaces indent, comma delimiter, strict validation, path expansion off.
     */
    public static final DecodeOptions DEFAULT = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
            MAX_ALLOWED_DEPTH, DEFAULT_MAX_ARRAY_SIZE, DEFAULT_MAX_STRING_LENGTH);

    /**
     * Creates DecodeOptions with default values.
     */
    public DecodeOptions() {
        this(2, Delimiter.COMMA, true, PathExpansion.OFF,
                MAX_ALLOWED_DEPTH, DEFAULT_MAX_ARRAY_SIZE, DEFAULT_MAX_STRING_LENGTH);
    }

    /**
     * Compact constructor with validation.
     */
    public DecodeOptions {
        if (indent < 0) {
            throw new IllegalArgumentException("indent must be non-negative, got: " + indent);
        }
        if (indent > MAX_ALLOWED_INDENT) {
            throw new IllegalArgumentException("indent must be <= " + MAX_ALLOWED_INDENT + ", got: " + indent);
        }
        delimiter = Objects.requireNonNull(delimiter, "delimiter cannot be null");
        if (maxDepth <= 0) {
            throw new IllegalArgumentException("maxDepth must be positive, got: " + maxDepth);
        }
        if (maxDepth > MAX_ALLOWED_DEPTH) {
            throw new IllegalArgumentException("maxDepth must be <= " + MAX_ALLOWED_DEPTH + ", got: " + maxDepth);
        }
        if (maxArraySize <= 0) {
            throw new IllegalArgumentException("maxArraySize must be positive, got: " + maxArraySize);
        }
        if (maxStringLength <= 0) {
            throw new IllegalArgumentException("maxStringLength must be positive, got: " + maxStringLength);
        }
    }

    /**
     * Creates DecodeOptions with custom indent, using default delimiter and strict
     * mode.
     * @param indent number of spaces per indentation level
     * @return a new DecodeOptions instance with the specified indent
     */
    public static DecodeOptions withIndent(final int indent) {
        return new DecodeOptions(indent, Delimiter.COMMA, true, PathExpansion.OFF,
                MAX_ALLOWED_DEPTH, DEFAULT_MAX_ARRAY_SIZE, DEFAULT_MAX_STRING_LENGTH);
    }

    /**
     * Creates DecodeOptions with custom delimiter, using default indent and strict
     * mode.
     * @param delimiter the delimiter to use for tabular arrays and inline primitive arrays
     * @return a new DecodeOptions instance with the specified delimiter
     */
    public static DecodeOptions withDelimiter(final Delimiter delimiter) {
        return new DecodeOptions(2, delimiter, true, PathExpansion.OFF,
                MAX_ALLOWED_DEPTH, DEFAULT_MAX_ARRAY_SIZE, DEFAULT_MAX_STRING_LENGTH);
    }

    /**
     * Creates DecodeOptions with custom strict mode, using default indent and
     * delimiter.
     * @param strict whether to enable strict validation mode
     * @return a new DecodeOptions instance with the specified strict mode
     */
    public static DecodeOptions withStrict(final boolean strict) {
        return new DecodeOptions(2, Delimiter.COMMA, strict, PathExpansion.OFF,
                MAX_ALLOWED_DEPTH, DEFAULT_MAX_ARRAY_SIZE, DEFAULT_MAX_STRING_LENGTH);
    }
}
