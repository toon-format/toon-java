package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.util.StringEscaper;
import dev.toonformat.jtoon.util.StringValidator;
import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import static dev.toonformat.jtoon.util.Constants.NULL_LITERAL;
import static dev.toonformat.jtoon.util.Constants.DOUBLE_QUOTE;

/**
 * Encodes primitive values and object keys for TOON format.
 * Delegates validation to StringValidator, escaping to StringEscaper,
 * and header formatting to HeaderFormatter.
 */
public final class PrimitiveEncoder {

    private static final int INITIAL_BUFFER_SIZE = 128;

    private PrimitiveEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encodes a primitive JsonNode value.
     *
     * @param value     the primitive value to encode
     * @param delimiter the delimiter to use (for string validation)
     * @return the encoded string representation
     */
    public static String encodePrimitive(final JsonNode value, final String delimiter) {
        return switch (value.getNodeType()) {
            case BOOLEAN -> String.valueOf(value.asBoolean());
            case NUMBER -> encodeNumber(value);
            case STRING -> encodeStringLiteral(value.asString(), delimiter);
            default -> NULL_LITERAL;
        };
    }

    /**
     * Encodes a number JsonNode to plain decimal format (no scientific notation).
     * Ensures LLM-safe output by converting all numbers to plain decimal
     * representation.
     */
    private static String encodeNumber(final JsonNode value) {
        if (value.isIntegralNumber()) {
            return value.asString();
        }

        final double doubleValue = value.asDouble();
        final BigDecimal decimal = BigDecimal.valueOf(doubleValue);
        final String plainString = decimal.toPlainString();

        return stripTrailingZeros(plainString);
    }

    /**
     * Strips trailing zeros from decimal numbers while preserving single zero after
     * decimal point.
     * Examples: "1.500" -> "1.5", "1.0" -> "1", "0.000001" -> "0.000001"
     */
    private static String stripTrailingZeros(final String value) {
        final int dotIndex = value.indexOf('.');
        if (dotIndex < 0) {
            return value;
        }

        int lastNonZero = value.length() - 1;
        while (lastNonZero > dotIndex && value.charAt(lastNonZero) == '0') {
            lastNonZero--;
        }

        if (lastNonZero == dotIndex) {
            return value.substring(0, dotIndex);
        }

        return value.substring(0, lastNonZero + 1);
    }

    /**
     * Encodes a string literal, quoting if necessary.
     * Delegates validation to StringValidator and escaping to StringEscaper.
     *
     * @param value     the string value to encode
     * @param delimiter the delimiter to use (for validation)
     * @return the encoded string, quoted if necessary
     */
    static String encodeStringLiteral(final String value, final String delimiter) {
        if (StringValidator.isSafeUnquoted(value, delimiter)) {
            return value;
        }

        return DOUBLE_QUOTE + StringEscaper.escape(value) + DOUBLE_QUOTE;
    }

    /**
     * Encodes an object key, quoting if necessary.
     * Delegates validation to StringValidator and escaping to StringEscaper.
     *
     * @param key the key to encode
     * @return the encoded key, quoted if necessary
     */
    public static String encodeKey(final String key) {
        if (StringValidator.isValidUnquotedKey(key)) {
            return key;
        }

        return DOUBLE_QUOTE + StringEscaper.escape(key) + DOUBLE_QUOTE;
    }

    /**
     * Joins encoded primitive values with the specified delimiter.
     *
     * @param values    the list of primitive values to join
     * @param delimiter the delimiter to use between values
     * @return the joined string of encoded values
     */
    public static String joinEncodedValues(final Collection<JsonNode> values, final String delimiter) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        final StringBuilder stringBuilder = new StringBuilder(INITIAL_BUFFER_SIZE);
        boolean first = true;
        for (final JsonNode node : values) {
            if (node == null) {
                continue;
            }
            if (!first) {
                stringBuilder.append(delimiter);
            }
            first = false;
            stringBuilder.append(encodePrimitive(node, delimiter));
        }
        return stringBuilder.toString();
    }

    /**
     * Formats a header for arrays and tables.
     * Delegates to HeaderFormatter for implementation.
     *
     * @param length       Array length
     * @param key          Optional key prefix
     * @param fields       Optional field names for tabular format
     * @param delimiter    The delimiter being used
     * @param lengthMarker Whether to include # marker before length
     * @return Formatted header string
     */
    public static String formatHeader(
        final int length,
        final String key,
        final List<String> fields,
        final String delimiter,
        final boolean lengthMarker) {
        return HeaderFormatter.format(length, key, fields, delimiter, lengthMarker);
    }
}
