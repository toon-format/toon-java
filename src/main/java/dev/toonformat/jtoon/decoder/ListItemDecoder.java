package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.util.Headers;
import dev.toonformat.jtoon.util.StringEscaper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_MARKER;
import static dev.toonformat.jtoon.util.Constants.OPEN_BRACKET;

/**
 * Handles decoding of TOON list item to JSON format.
 */
public final class ListItemDecoder {

    // Spec §6: a keyless array header is valid as a list item only in its
    // plain form ([N]: or []); a fields-bearing ([N]{...}:) or keyed
    // ([N:]{...}:) keyless header is a defect.
    private static final Pattern KEYLESS_FIELDS_HEADER = Pattern.compile("^\\[[^]]*]\\s*\\{");

    private ListItemDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Processes a single list array item if it matches the expected depth.
     *
     * @param line      the line string to parse
     * @param lineDepth the depth of the line
     * @param depth     the depth of list array item
     * @param result    the stored result of each list item parse
     * @param context   decode an object to deal with lines, delimiter and options
     */
    public static void processListArrayItem(final String line, final int lineDepth, final int depth,
                                            final Collection<Object> result, final DecodeContext context) {
        if (lineDepth == depth + 1) {
            final String content = line.substring((depth + 1) * context.options.indent());

            if (content.startsWith(LIST_ITEM_MARKER)) {
                result.add(parseListItem(content, depth, context));
            } else {
                context.currentLine++;
            }
        } else {
            context.currentLine++;
        }
    }

    /**
     * Parses a single list item starting with "- ".
     * Item can be a scalar value or an object with nested fields.
     *
     * @param content the content string to parse
     * @param depth   the depth of list item
     * @param context decode an object to deal with lines, delimiter and options
     * @return parsed item (scalar value or object)
     */
    static Object parseListItem(final String content, final int depth, final DecodeContext context) {
        final String itemContent = extractItemContent(content);

        // Handle empty item: just "-"
        if (itemContent.isEmpty()) {
            context.currentLine++;
            return new LinkedHashMap<>();
        }

        // Check for standalone array (e.g., "[2]: 1,2")
        if (itemContent.startsWith(OPEN_BRACKET)) {
            return parseStandaloneArrayItem(itemContent, depth, context);
        }

        // Check for keyed array pattern (e.g., "tags[3]: a,b,c" or "data[2]{id}: ...")
        final Headers.KeyedHeaderMatch keyedHeader = Headers.matchKeyedArrayHeader(itemContent);
        if (keyedHeader != null && keyedHeader.keyed()) {
            return parseKeyedTabularListItem(itemContent, keyedHeader, depth, context);
        }
        if (keyedHeader != null && !isKeyedMismatchFallThrough(itemContent, keyedHeader, context)) {
            return parseKeyedArrayListItem(itemContent, keyedHeader, depth, context);
        }

        final int colonIdx = DecodeHelper.findUnquotedColon(itemContent);

        // Simple scalar: - value
        if (colonIdx <= 0) {
            context.currentLine++;
            return PrimitiveDecoder.parse(itemContent, context);
        }

        return parseObjectListItem(itemContent, colonIdx, depth, context);
    }

    /**
     * Extracts the item content past the leading "- " marker.
     *
     * @param content the list item line content
     * @return the trimmed item content, or an empty string
     */
    private static String extractItemContent(final String content) {
        if (content.length() > 2) {
            return content.substring(2).trim();
        }
        return "";
    }

    /**
     * Parses a standalone array item, validating the keyless header form.
     *
     * @param itemContent the item content starting with the bracket segment
     * @param depth       the depth of the list item
     * @param context     decode an object to deal with lines, delimiter and options
     * @return the parsed array
     */
    private static Object parseStandaloneArrayItem(final String itemContent, final int depth,
            final DecodeContext context) {
        // Keyless headers are valid as list items only without a field
        // list; [2]{x}: and [2:]{v}: are defects (§5, §6)
        if (context.options.strict() && KEYLESS_FIELDS_HEADER.matcher(itemContent).find()) {
            throw new IllegalArgumentException(
                "Keyless array header with field list only valid at document root at line "
                    + (context.currentLine + 1));
        }
        final Delimiter nestedArrayDelimiter = ArrayDecoder.extractDelimiterFromHeader(itemContent, context);
        return ArrayDecoder.parseArrayWithDelimiter(itemContent, depth + 1, nestedArrayDelimiter, context);
    }

    /**
     * Returns whether a keyed header with a mismatched tabular delimiter
     * falls through to ordinary key-value parsing in non-strict mode (§6).
     *
     * @param itemContent the item content
     * @param keyedHeader the matched keyed header
     * @param context     decode an object to deal with lines, delimiter and options
     * @return true when the line falls through to key-value parsing
     */
    private static boolean isKeyedMismatchFallThrough(final String itemContent,
            final Headers.KeyedHeaderMatch keyedHeader, final DecodeContext context) {
        return !keyedHeader.keyed()
            && !context.options.strict()
            && ArrayDecoder.hasTabularDelimiterMismatch(itemContent.substring(keyedHeader.keyEnd()));
    }

    /**
     * Parses a keyed tabular list item, keeping its entry rows at depth + 3.
     *
     * @param itemContent the item content
     * @param keyedHeader the matched keyed header
     * @param depth       the depth of the list item
     * @param context     decode an object to deal with lines, delimiter and options
     * @return the parsed item map
     */
    private static Map<String, Object> parseKeyedTabularListItem(final String itemContent,
            final Headers.KeyedHeaderMatch keyedHeader, final int depth, final DecodeContext context) {
        final String originalKey = keyedHeader.key().trim();
        final String key = StringEscaper.unescape(originalKey);
        final Map<String, Object> item = new LinkedHashMap<>();

        // Spec §9.5/§10: a keyed tabular object on the hyphen line keeps
        // its entry rows at document depth + 3 (header on depth + 1) and
        // its sibling fields at depth + 2.
        final Object keyedValue = KeyedObjectDecoder.parseKeyedTabularObject(
            itemContent, keyedHeader, depth + 3, context);
        DecodeHelper.checkDuplicateKey(item, key, context);
        item.put(key, keyedValue);

        // parseKeyedTabularObject manages currentLine: entry rows and the
        // sibling lines that follow them are left to parseListItemFields.
        parseListItemFields(item, depth, context);

        return item;
    }

    /**
     * Parses a keyed array list item ({@code - tags[2]: a,b}).
     *
     * @param itemContent the item content
     * @param keyedHeader the matched keyed header
     * @param depth       the depth of the list item
     * @param context     decode an object to deal with lines, delimiter and options
     * @return the parsed item map
     */
    private static Map<String, Object> parseKeyedArrayListItem(final String itemContent,
            final Headers.KeyedHeaderMatch keyedHeader, final int depth, final DecodeContext context) {
        final String originalKey = keyedHeader.key().trim();
        final String key = StringEscaper.unescape(originalKey);
        final String arrayHeader = itemContent.substring(keyedHeader.keyEnd());

        final Delimiter nestedArrayDelimiter = ArrayDecoder.extractDelimiterFromHeader(arrayHeader, context);
        final List<Object> arrayValue = ArrayDecoder.parseArrayWithDelimiter(
            arrayHeader, depth + 2, nestedArrayDelimiter, context
        );

        final Map<String, Object> item = new LinkedHashMap<>();
        DecodeHelper.checkDuplicateKey(item, key, context);
        item.put(key, arrayValue);

        // parseArrayWithDelimiter manages currentLine correctly:
        // - For inline arrays, it increments currentLine
        // - For multi-line arrays (list/tabular), the array parsers leave currentLine
        // at the line after the array
        // So we don't need to increment here. Just parse additional fields.
        parseListItemFields(item, depth, context);

        return item;
    }

    /**
     * Parses an object list item ({@code - key: value}).
     *
     * @param itemContent the item content
     * @param colonIdx    the index of the key-value colon
     * @param depth       the depth of the list item
     * @param context     decode an object to deal with lines, delimiter and options
     * @return the parsed item map
     */
    private static Map<String, Object> parseObjectListItem(final String itemContent, final int colonIdx,
            final int depth, final DecodeContext context) {
        // Object item: - key: value
        final String key = StringEscaper.unescape(itemContent.substring(0, colonIdx).trim());
        final String value = itemContent.substring(colonIdx + 1).trim();

        context.currentLine++;

        final Map<String, Object> item = new LinkedHashMap<>();
        final Object parsedValue;
        // If no next line exists, handle a simple case
        if (context.currentLine >= context.lines.length) {
            parsedValue = value.isBlank() ? new LinkedHashMap<>() : PrimitiveDecoder.parse(value, context);
        } else {
            // List item is at depth + 1, so pass depth + 1 to parseObjectItemValue
            parsedValue = ObjectDecoder.parseObjectItemValue(value, depth + 1, context);
        }
        DecodeHelper.checkDuplicateKey(item, key, context);
        item.put(key, parsedValue);
        parseListItemFields(item, depth, context);

        return item;
    }

    /**
     * Parses additional fields for a list item object.
     *
     * @param item    the item to parse
     * @param depth   the depth of the item
     * @param context decode an object to deal with lines, delimiter and options
     */
    private static void parseListItemFields(final Map<String, Object> item,
            final int depth, final DecodeContext context) {
        while (context.currentLine < context.lines.length) {
            final String line = context.lines[context.currentLine];
            final int lineDepth = DecodeHelper.getDepth(line, context);

            if (lineDepth < depth + 2) {
                return;
            }

            if (lineDepth == depth + 2) {
                processListItemFieldLine(item, line, depth, context);
            } else {
                // lineDepth > depth + 2: over-indented line (§14.2)
                DecodeHelper.processOverIndentedLine(context, lineDepth);
            }
        }
    }

    /**
     * Processes a sibling field line of a list item, falling back to
     * key-value parsing when the keyed array pattern does not match.
     *
     * @param item    the item to fill
     * @param line    the field line
     * @param depth   the depth of the item
     * @param context decode an object to deal with lines, delimiter and options
     */
    private static void processListItemFieldLine(final Map<String, Object> item, final String line,
            final int depth, final DecodeContext context) {
        final String fieldContent = line.substring((depth + 2) * context.options.indent());

        // Try to parse as a keyed array first, then as a key-value pair
        boolean wasParsed = KeyDecoder.parseKeyedArrayField(fieldContent, item, depth, context);
        if (!wasParsed) {
            wasParsed = KeyDecoder.parseKeyValueField(fieldContent, item, depth, context);
        }

        // If neither pattern matched, skip this line to avoid an infinite loop
        if (!wasParsed) {
            context.currentLine++;
        }
    }
}
