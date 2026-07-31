package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.util.Headers;
import dev.toonformat.jtoon.util.StringEscaper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles decoding of keyed tabular objects (§9.5) to JSON format.
 *
 * <p>A keyed tabular object is a keyed array header with a keyed marker
 * ({@code key[N:]{field…}:} or, at document root only, {@code [N:]{field…}:})
 * followed by entry rows at header depth + 1, each starting with an entry key
 * and a colon before its cell values.</p>
 */
public final class KeyedObjectDecoder {

    private KeyedObjectDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parses a keyed tabular object, e.g. {@code servers[2:]{host,port}:}
     * followed by entry rows {@code alpha: a.example.com,8080}.
     *
     * @param content    the header line content
     * @param header     the keyed header match for the line
     * @param entryDepth the depth of the entry rows (header depth + 1; for
     *                   hyphen-line items one level below the header, §10)
     * @param context    decode an object to deal with lines, delimiter and options
     * @return the parsed keyed object, an entry-keyed map of row maps
     */
    static Map<String, Object> parseKeyedTabularObject(final String content,
            final Headers.KeyedHeaderMatch header, final int entryDepth, final DecodeContext context) {
        if (header.fieldsStart() < 0) {
            throw new IllegalArgumentException(
                "Keyed header requires a field list at line " + (context.currentLine + 1));
        }
        if (!content.substring(header.headerEnd() + 1).isBlank()) {
            throw new IllegalArgumentException(
                "Inline content after keyed header at line " + (context.currentLine + 1));
        }

        final String fieldsSpec = content.substring(header.fieldsStart() + 1, header.headerEnd() - 1);
        final Delimiter arrayDelimiter = delimiterFromChar(header.delimiter(), context);

        final List<TabularArrayDecoder.FieldNode> fields =
            TabularArrayDecoder.parseTabularKeys(fieldsSpec, arrayDelimiter, context);

        // Spec §9.3: a duplicate field name within one field list is a header
        // defect, diagnosed from the header line alone.
        if (context.options.strict()) {
            TabularArrayDecoder.validateNoDuplicateFields(fields, context);
        }

        final Map<String, Object> result = new LinkedHashMap<>();
        context.currentLine++;

        while (context.currentLine < context.lines.length) {
            final String line = context.lines[context.currentLine];

            if (DecodeHelper.isBlankLine(line)) {
                final int nextNonBlank = DecodeHelper.findNextNonBlankLine(context.currentLine + 1, context);
                if (nextNonBlank >= context.lines.length) {
                    break;
                }
                final int nextDepth = DecodeHelper.getDepth(context.lines[nextNonBlank], context);
                if (nextDepth <= entryDepth - 1) {
                    break;
                }
                // Spec §12: blank lines between the header and the first entry
                // row are accepted; later blank lines inside the object are a
                // defect in strict mode.
                if (!result.isEmpty() && context.options.strict()) {
                    throw new IllegalArgumentException(
                        "Blank line inside keyed object at line " + (context.currentLine + 1));
                }
                context.currentLine++;
                continue;
            }

            final int lineDepth = DecodeHelper.getDepth(line, context);
            if (lineDepth < entryDepth) {
                break;
            }
            if (lineDepth > entryDepth) {
                // A line deeper than the entry depth belongs to no scope (§14.2)
                if (context.options.strict()) {
                    throw new IllegalArgumentException(
                        "Over-indented line at " + (context.currentLine + 1) + " (depth " + lineDepth + ")");
                }
                context.currentLine++;
                continue;
            }

            final String entryContent = line.substring(entryDepth * context.options.indent());
            // Spec §9.5: an entry row splits at its first unquoted colon; the
            // remainder is parsed as a tabular row with the active delimiter.
            final int colonIdx = DecodeHelper.findUnquotedColon(entryContent);
            if (colonIdx <= 0) {
                if (context.options.strict()) {
                    throw new IllegalArgumentException(
                        "Missing colon in keyed entry at line " + (context.currentLine + 1));
                }
                context.currentLine++;
                continue;
            }

            final String entryKey = StringEscaper.unescape(entryContent.substring(0, colonIdx).trim());
            final Map<String, Object> entry = TabularArrayDecoder.parseTabularRow(
                entryContent.substring(colonIdx + 1), fields, arrayDelimiter, context);

            DecodeHelper.checkDuplicateKey(result, entryKey, context);
            result.put(entryKey, entry);
            context.currentLine++;
        }

        // Spec §9.5: the declared entry count must match in strict mode
        if (context.options.strict() && result.size() != header.declaredLength()) {
            throw new IllegalArgumentException(
                String.format("Keyed object entry count (%d) does not match declared length (%d)",
                              result.size(), header.declaredLength()));
        }
        return result;
    }

    private static Delimiter delimiterFromChar(@org.jspecify.annotations.Nullable final Character delimiter,
            final DecodeContext context) {
        if (delimiter == null) {
            return context.delimiter;
        }
        final char c = delimiter;
        if (c == Delimiter.TAB.getValue()) {
            return Delimiter.TAB;
        }
        if (c == Delimiter.PIPE.getValue()) {
            return Delimiter.PIPE;
        }
        return Delimiter.COMMA;
    }
}
