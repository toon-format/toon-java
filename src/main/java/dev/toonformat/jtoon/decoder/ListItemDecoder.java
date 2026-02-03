package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.util.StringEscaper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static dev.toonformat.jtoon.util.Constants.LIST_ITEM_MARKER;
import static dev.toonformat.jtoon.util.Constants.OPEN_BRACKET;
import static dev.toonformat.jtoon.util.Headers.KEYED_ARRAY_PATTERN;

/**
 * Handles decoding of TOON list item to JSON format.
 */
public final class ListItemDecoder {

    private ListItemDecoder() {throw new UnsupportedOperationException("Utility class cannot be instantiated");}

    /**
     * Processes a single list array item if it matches the expected depth.
     *
     * @param line      the line string to parse
     * @param lineDepth the depth of the line
     * @param depth     the depth of list array item
     * @param result    the stored result of each list item parse
     * @param context   decode an object to deal with lines, delimiter and options
     */
    public static void processListArrayItem(String line, int lineDepth, int depth,
                                            List<Object> result, DecodeContext context) {
        if (lineDepth == depth + 1) {
            String content = line.substring((depth + 1) * context.options.indent());

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
    public static Object parseListItem(String content, int depth, DecodeContext context) {
        // Handle empty item: just "-" or "- "
        String itemContent;
        if (content.length() > 2) {
            itemContent = content.substring(2).trim();
        } else {
            itemContent = "";
        }

        // Handle empty item: just "-"
        if (itemContent.isEmpty()) {
            context.currentLine++;
            return new LinkedHashMap<>();
        }

        // Check for standalone array (e.g., "[2]: 1,2")
        if (itemContent.startsWith(OPEN_BRACKET)) {
            // For nested arrays in list items, default to comma delimiter if not specified
            Delimiter nestedArrayDelimiter = ArrayDecoder.extractDelimiterFromHeader(itemContent, context);
            // parseArrayWithDelimiter handles currentLine increment internally
            // For inline arrays, it increments. For multi-line arrays, parseListArray
            // handles it.
            // We need to increment here only if it was an inline array that we just parsed
            // Actually, parseArrayWithDelimiter always handles currentLine, so we don't
            // need to increment
            return ArrayDecoder.parseArrayWithDelimiter(itemContent, depth + 1, nestedArrayDelimiter, context);
        }

        // Check for keyed array pattern (e.g., "tags[3]: a,b,c" or "data[2]{id}: ...")
        Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(itemContent);
        if (keyedArray.matches()) {
            String originalKey = keyedArray.group(1).trim();
            String key = StringEscaper.unescape(originalKey);
            String arrayHeader = itemContent.substring(keyedArray.group(1).length());

            // For nested arrays in list items, default to comma delimiter if not specified
            Delimiter nestedArrayDelimiter = ArrayDecoder.extractDelimiterFromHeader(arrayHeader, context);
            List<Object> arrayValue = ArrayDecoder.parseArrayWithDelimiter(arrayHeader, depth + 2, nestedArrayDelimiter, context);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put(key, arrayValue);

            // parseArrayWithDelimiter manages currentLine correctly:
            // - For inline arrays, it increments currentLine
            // - For multi-line arrays (list/tabular), the array parsers leave currentLine
            // at the line after the array
            // So we don't need to increment here. Just parse additional fields.
            parseListItemFields(item, depth, context);

            return item;
        }

        int colonIdx = DecodeHelper.findUnquotedColon(itemContent);

        // Simple scalar: - value
        if (colonIdx <= 0) {
            context.currentLine++;
            return PrimitiveDecoder.parse(itemContent);
        }

        // Object item: - key: value
        String key = StringEscaper.unescape(itemContent.substring(0, colonIdx).trim());
        String value = itemContent.substring(colonIdx + 1).trim();

        context.currentLine++;

        Map<String, Object> item = new LinkedHashMap<>();
        Object parsedValue;
        // If no next line exists, handle a simple case
        if (context.currentLine >= context.lines.length) {
            parsedValue = value.trim().isEmpty() ? new LinkedHashMap<>() : PrimitiveDecoder.parse(value);
        } else {
            // List item is at depth + 1, so pass depth + 1 to parseObjectItemValue
            parsedValue = ObjectDecoder.parseObjectItemValue(value, depth + 1, context);
        }
        item.put(key, parsedValue);
        parseListItemFields(item, depth, context);

        return item;
    }

    /**
     * Parses additional fields for a list item object.
     *
     * @param item    the item to parse
     * @param depth   the depth of the item
     * @param context decode an object to deal with lines, delimiter and options     *
     */
    private static void parseListItemFields(Map<String, Object> item, int depth, DecodeContext context) {
        while (context.currentLine < context.lines.length) {
            String line = context.lines[context.currentLine];
            int lineDepth = DecodeHelper.getDepth(line, context);

            if (lineDepth < depth + 2) {
                return;
            }

            if (lineDepth == depth + 2) {
                String fieldContent = line.substring((depth + 2) * context.options.indent());

                // Try to parse as a keyed array first, then as a key-value pair
                boolean wasParsed = KeyDecoder.parseKeyedArrayField(fieldContent, item, depth, context);
                if (!wasParsed) {
                    wasParsed = KeyDecoder.parseKeyValueField(fieldContent, item, depth, context);
                }

                // If neither pattern matched, skip this line to avoid an infinite loop
                if (!wasParsed) {
                    context.currentLine++;
                }
            } else {
                // lineDepth > depth + 2, skip this line
                context.currentLine++;
            }
        }
    }
}
