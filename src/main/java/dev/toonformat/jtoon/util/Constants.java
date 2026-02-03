package dev.toonformat.jtoon.util;

/**
 * Constants used throughout the JToon encoding process.
 */
public final class Constants {

    // List markers
    /** Marker used to indicate list items in TOON format. */
    public static final String LIST_ITEM_MARKER = "-";
    /** Prefix used for list items in TOON format. */
    public static final String LIST_ITEM_PREFIX = "- ";

    // Structural characters
    /** Comma delimiter character. */
    public static final String COMMA = ",";
    /** Colon separator character used in key-value pairs. */
    public static final String COLON = ":";
    /** Space character. */
    public static final String SPACE = " ";
    /** Dot character. */
    public static final String DOT = ".";

    // Brackets and braces
    /** Opening bracket character for arrays. */
    public static final String OPEN_BRACKET = "[";
    /** Closing bracket character for arrays. */
    public static final String CLOSE_BRACKET = "]";
    /** Opening brace character for objects. */
    public static final String OPEN_BRACE = "{";
    /** Closing brace character for objects. */
    public static final String CLOSE_BRACE = "}";

    // Literals
    /** String representation of null value. */
    public static final String NULL_LITERAL = "null";
    /** String representation of boolean true value. */
    public static final String TRUE_LITERAL = "true";
    /** String representation of boolean false value. */
    public static final String FALSE_LITERAL = "false";

    // Escape characters
    /** Backslash escape character. */
    public static final char BACKSLASH = '\\';
    /** Double quote character used for string literals. */
    public static final char DOUBLE_QUOTE = '"';

    private Constants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
