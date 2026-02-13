package dev.toonformat.jtoon.encoder;

import java.util.Collection;
import java.util.List;
import static dev.toonformat.jtoon.util.Constants.COLON;
import static dev.toonformat.jtoon.util.Constants.OPEN_BRACKET;
import static dev.toonformat.jtoon.util.Constants.COMMA;
import static dev.toonformat.jtoon.util.Constants.OPEN_BRACE;
import static dev.toonformat.jtoon.util.Constants.CLOSE_BRACE;
import static dev.toonformat.jtoon.util.Constants.CLOSE_BRACKET;
import static dev.toonformat.jtoon.util.Constants.HASHTAG;

/**
 * Formats headers for arrays and tables in TOON format.
 */
public final class HeaderFormatter {

    private HeaderFormatter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Configuration for header formatting.
     * 
     * @param length       Array or table length
     * @param key          Optional key prefix
     * @param fields       Optional field names for tabular format
     * @param delimiter    The delimiter being used
     * @param lengthMarker Whether to include # marker before length
     */
    public record HeaderConfig(
            int length,
            String key,
            List<String> fields,
            String delimiter,
            boolean lengthMarker) {
    }

    /**
     * Formats a header for arrays and tables.
     * 
     * @param config Header configuration
     * @return Formatted header string
     */
    static String format(final HeaderConfig config) {
        final StringBuilder header = new StringBuilder();

        appendKeyIfPresent(header, config.key());
        appendArrayLength(header, config.length(), config.delimiter(), config.lengthMarker());
        appendFieldsIfPresent(header, config.fields(), config.delimiter());
        header.append(COLON);

        return header.toString();
    }

    /**
     * Legacy method for backward compatibility.
     * Delegates to the record-based format method.
     * @param length the array or table length
     * @param key optional key prefix
     * @param fields optional field names for tabular format
     * @param delimiter the delimiter being used
     * @param lengthMarker whether to include # marker before length
     * @return formatted header string
     */
    public static String format(
            final int length,
            final String key,
            final List<String> fields,
            final String delimiter,
            final boolean lengthMarker) {
        final HeaderConfig config = new HeaderConfig(length, key, fields, delimiter, lengthMarker);
        return format(config);
    }

    private static void appendKeyIfPresent(final StringBuilder header, final String key) {
        if (key != null) {
            header.append(PrimitiveEncoder.encodeKey(key));
        }
    }

    private static void appendArrayLength(
            final StringBuilder header,
            final int length,
            final String delimiter,
            final boolean lengthMarker) {
        header.append(OPEN_BRACKET);
        
        if (lengthMarker) {
            header.append(HASHTAG);
        }
        
        header.append(length);
        appendDelimiterIfNotDefault(header, delimiter);
        header.append(CLOSE_BRACKET);
    }

    private static void appendDelimiterIfNotDefault(final StringBuilder header, final String delimiter) {
        if (!COMMA.equals(delimiter)) {
            header.append(delimiter);
        }
    }

    private static void appendFieldsIfPresent(
            final StringBuilder header,
            final Collection<String> fields,
            final String delimiter) {
        if (fields == null || fields.isEmpty()) {
            return;
        }

        header.append(OPEN_BRACE);
        header.append(formatFields(fields, delimiter));
        header.append(CLOSE_BRACE);
    }

    private static String formatFields(final Collection<String> fields, final String delimiter) {
        return fields.stream()
                .map(PrimitiveEncoder::encodeKey)
                .reduce((a, b) -> a + delimiter + b)
                .orElse("");
    }
}

