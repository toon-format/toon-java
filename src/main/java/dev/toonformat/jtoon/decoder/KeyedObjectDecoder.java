package dev.toonformat.jtoon.decoder;

import org.jspecify.annotations.Nullable;
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
        validateKeyedHeader(header, context);

        final String fieldsSpec = content.substring(header.fieldsStart() + 1, header.headerEnd() - 1);
        final Delimiter arrayDelimiter = delimiterFromChar(header.delimiter(), context);

        final List<TabularArrayDecoder.FieldNode> fields =
            TabularArrayDecoder.parseTabularKeys(fieldsSpec, arrayDelimiter);

        // Spec §9.3: a duplicate field name within one field list is a header
        // defect, diagnosed from the header line alone.
        if (context.options.strict()) {
            TabularArrayDecoder.validateNoDuplicateFields(fields, context);
        }

        final Map<String, Object> result = new LinkedHashMap<>();
        context.currentLine++;
        final int headerDepth = entryDepth - 1;
        final int rowDepth = DecodeHelper.findContentDepth(headerDepth, context);

        while (context.currentLine < context.lines.length) {
            final LineHandling handling = handleNextLine(result, headerDepth, rowDepth, context);
            if (handling == LineHandling.STOP) {
                break;
            }
            if (handling == LineHandling.PROCESS) {
                processEntryLine(context.lines[context.currentLine], rowDepth,
                    fields, arrayDelimiter, result, context);
                context.currentLine++;
            }
        }

        ArrayDecoder.validateArraySize(result.size(), context.options.maxArraySize());
        // Spec §9.5: the declared entry count must match in strict mode
        if (context.options.strict() && result.size() != header.declaredLength()) {
            throw new IllegalArgumentException(
                String.format("Keyed object entry count (%d) does not match declared length (%d)",
                              result.size(), header.declaredLength()));
        }
        return result;
    }

    /**
     * Validates the keyed header shape: a field list must be present.
     *
     * @param header  the keyed header match for the line
     * @param context decode an object to deal with lines, delimiter and options
     * @throws IllegalArgumentException for a defective header
     */
    private static void validateKeyedHeader(final Headers.KeyedHeaderMatch header, final DecodeContext context) {
        if (header.fieldsStart() < 0) {
            throw new IllegalArgumentException(
                "Keyed header requires a field list at line " + (context.currentLine + 1));
        }
    }

    /**
     * Returns whether parsing stops at a blank line: at the end of the input
     * or when the next non-blank line sits outside the keyed object. Blank
     * lines inside the object are rejected in strict mode (§12).
     *
     * @param result      the rows parsed so far
     * @param headerDepth the depth of the keyed header
     * @param context     decode an object to deal with lines, delimiter and options
     * @return true when the blank line terminates the object
     */
    private static boolean shouldStopAtBlankLine(final Map<String, Object> result, final int headerDepth,
            final DecodeContext context) {
        final int nextNonBlank = DecodeHelper.findNextNonBlankLine(context.currentLine + 1, context);
        if (nextNonBlank >= context.lines.length) {
            return true; // EOF - terminate
        }
        final int nextDepth = DecodeHelper.getDepth(context.lines[nextNonBlank], context);
        if (nextDepth <= headerDepth) {
            return true; // outside the object - terminate
        }
        // Spec §12: blank lines between the header and the first entry
        // row are accepted; later blank lines inside the object are a
        // defect in strict mode.
        if (!result.isEmpty() && context.options.strict()) {
            throw new IllegalArgumentException(
                "Blank line inside keyed object at line " + (context.currentLine + 1));
        }
        return false;
    }

    /**
     * How the next line of a keyed object is handled by the parse loop.
     */
    private enum LineHandling { STOP, SKIP, PROCESS }

    /**
     * Classifies the current line of a keyed tabular object: terminates the
     * object at blank-line/EOF boundaries or shallower lines, skips blank
     * lines, rejects over-indented lines (§14.2), and passes entry rows through.
     *
     * @param result      the rows parsed so far
     * @param headerDepth the depth of the keyed header
     * @param entryDepth  the depth of the entry rows
     * @param context     decode an object to deal with lines, delimiter and options
     * @return the handling to apply to the current line
     */
    private static LineHandling handleNextLine(final Map<String, Object> result, final int headerDepth,
            final int entryDepth, final DecodeContext context) {
        final String line = context.lines[context.currentLine];

        if (DecodeHelper.isBlankLine(line)) {
            if (shouldStopAtBlankLine(result, headerDepth, context)) {
                return LineHandling.STOP;
            }
            context.currentLine++;
            return LineHandling.SKIP;
        }

        final int lineDepth = DecodeHelper.getDepth(line, context);
        if (lineDepth <= headerDepth) {
            return LineHandling.STOP;
        }
        if (lineDepth != entryDepth) {
            throw DecodeHelper.overIndentedLineError(context, lineDepth);
        }
        return LineHandling.PROCESS;
    }

    /**
     * Parses one entry row of a keyed tabular object, splitting the row at
     * its first unquoted colon (§9.5). A row without a colon is rejected.
     *
     * @param line          the entry row line
     * @param entryDepth    the depth of the entry rows
     * @param fields        the declared field nodes
     * @param arrayDelimiter the active delimiter
     * @param result        the entry-keyed result map
     * @param context       decode an object to deal with lines, delimiter and options
     */
    private static void processEntryLine(final String line, final int entryDepth,
            final List<TabularArrayDecoder.FieldNode> fields, final Delimiter arrayDelimiter,
            final Map<String, Object> result, final DecodeContext context) {
        final String entryContent = line.substring(entryDepth * context.options.indent());
        // Spec §9.5: an entry row splits at its first unquoted colon; the
        // remainder is parsed as a tabular row with the active delimiter.
        final int colonIdx = DecodeHelper.findUnquotedColon(entryContent);
        if (colonIdx < 0) {
            throw new IllegalArgumentException(
                "Missing colon in keyed entry at line " + (context.currentLine + 1));
        }

        final String rawEntryKey = DecodeHelper.trimSpaces(entryContent.substring(0, colonIdx));
        DecodeHelper.validateQuotedTokenBoundary(rawEntryKey);
        final String entryKey = StringEscaper.unescape(rawEntryKey);
        final Map<String, Object> entry = TabularArrayDecoder.parseTabularRow(
            entryContent.substring(colonIdx + 1), fields, arrayDelimiter, context);

        DecodeHelper.checkDuplicateKey(result, entryKey, context);
        result.put(entryKey, entry);
    }

    private static Delimiter delimiterFromChar(@Nullable final Character delimiter,
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
