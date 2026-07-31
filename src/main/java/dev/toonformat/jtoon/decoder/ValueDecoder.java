package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.util.Headers;
import dev.toonformat.jtoon.util.ObjectMapperSingleton;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import static dev.toonformat.jtoon.util.Constants.NULL_LITERAL;
import static dev.toonformat.jtoon.util.Constants.OPEN_BRACKET;

/**
 * Main decoder for converting TOON-formatted strings to Java objects.
 *
 * <p>
 * Implements a line-by-line parser with indentation-based depth tracking.
 * Delegates primitive type inference to {@link PrimitiveDecoder}.
 * </p>
 *
 * <h2>Parsing Strategy:</h2>
 * <ul>
 * <li>Split input into lines</li>
 * <li>Track current line position and indentation depth</li>
 * <li>Use regex patterns to detect structure (arrays, objects, primitives)</li>
 * <li>Recursively process nested structures</li>
 * </ul>
 *
 * @see DecodeOptions
 * @see PrimitiveDecoder
 */
public final class ValueDecoder {

    private static final ObjectMapper MAPPER = ObjectMapperSingleton.getInstance();
    private static final int BOM_CHARACTER = 0xFEFF;

    private ValueDecoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Decodes a TOON-formatted string to a Java object.
     *
     * @param toon    TOON-formatted input string
     * @param options parsing options (delimiter, indentation, strict mode)
     * @return parsed object (Map, List, primitive, or null)
     * @throws IllegalArgumentException if strict mode is enabled and input is
     *                                  invalid
     */
    @Nullable
    public static Object decode(final String toon, final DecodeOptions options) {
        try {
            return decodeInternal(toon, options);
        } catch (FatalDecodeException e) {
            // Spec §5.2/§7.4: bare scalars outside root primitive position and
            // characters after a closing quote are errors in strict and
            // non-strict mode alike; lenient mode must not swallow them.
            throw e;
        } catch (IllegalArgumentException e) {
            if (!options.strict()) {
                return null;
            }
            throw e;
        }
    }

    @Nullable
    private static Object decodeInternal(final String toon, final DecodeOptions options) {
        if (toon == null) {
            return new LinkedHashMap<>();
        }

        // Spec §5.1: a single U+FEFF at the very start of the document is a
        // byte-order mark, not content; remove it before any processing.
        String input = toon;
        if (!input.isEmpty() && input.charAt(0) == BOM_CHARACTER) {
            input = input.substring(1);
        }

        if (input.isBlank()) {
            return new LinkedHashMap<>();
        }

        // Special case: if input is exactly "null", return null
        final String trimmed = input.trim();
        if (NULL_LITERAL.equals(trimmed)) {
            return null;
        }
        if ("[]".equals(trimmed)) {
            return java.util.Collections.emptyList();
        }

        //set an own decode context
        final DecodeContext context = new DecodeContext();
        final String[] rawLines = input.split("\r?\n", -1);
        // Spec §12: trailing spaces at the end of a line are not part of its content;
        // strip them per line before classification. Only characters after the last
        // non-space character are removed, so trailing spaces inside quoted strings
        // (e.g. key: "a ") are preserved.
        // Spec §5.1: a line whose first non-space character (U+0020 only) is '#'
        // is a full-line comment; it is discarded before any structural
        // interpretation. A tab before '#' disqualifies the line, and a '#'
        // anywhere else is data, not a comment.
        final List<String> contentLines = new ArrayList<>(rawLines.length);
        for (int i = 0; i < rawLines.length; i++) {
            final String stripped = rawLines[i].stripTrailing();
            if (!isCommentLine(stripped)) {
                contentLines.add(stripped);
            }
        }
        context.lines = contentLines.toArray(new String[0]);
        context.options = options;
        context.delimiter = options.delimiter();

        // Spec §5.1: a document of only comments and blank lines is an empty object
        boolean allBlank = true;
        for (final String line : context.lines) {
            if (!line.isBlank()) {
                allBlank = false;
                break;
            }
        }
        if (allBlank) {
            return new LinkedHashMap<>();
        }

        final int lineIndex = context.currentLine;
        final String line = context.lines[lineIndex];
        final int depth = DecodeHelper.getDepth(line, context);

        if (depth > 0) {
            if (context.options.strict()) {
                throw new IllegalArgumentException("Unexpected indentation at line " + lineIndex);
            }
            return new LinkedHashMap<>();
        }

        final Object result;
        // Handle standalone arrays: [2]:
        if (!line.isEmpty() && line.charAt(0) == OPEN_BRACKET.charAt(0)) {
            // Spec §5/§9.5: a keyed marker in the bracket segment makes a
            // keyless header a keyed tabular object, not an array.
            final Headers.KeyedHeaderMatch keylessHeader = Headers.matchKeylessKeyedHeader(line);
            if (keylessHeader != null && keylessHeader.keyed()) {
                result = KeyedObjectDecoder.parseKeyedTabularObject(line, keylessHeader, depth + 1, context);
            } else {
                result = ArrayDecoder.parseArray(line, depth, context);
            }
        } else {
            // Handle keyed arrays: items[2]{id,name}:
            final Headers.KeyedHeaderMatch keyedHeader = Headers.matchKeyedArrayHeader(line);
            if (keyedHeader != null) {
                result = KeyDecoder.parseKeyedArrayValue(keyedHeader, line, depth, context);
            } else {
                // Handle key-value pairs: name: Ada
                final int colonIdx = DecodeHelper.findUnquotedColon(line);
                if (colonIdx > 0) {
                    if (context.options.strict()) {
                        final String key = line.substring(0, colonIdx).trim();
                        // In strict mode, reject keys with unquoted brackets that didn't match
                        // KEYED_ARRAY_PATTERN. This catches:
                        //   - extra brackets between bracket segment and colon (foo[1][bar])
                        //   - text between bracket segment and colon (foo[2]extra)
                        //   - non-integer bracket segment (foo[bar])
                        //   - negative bracket length (items[-1])
                        //   - whitespace between bracket segment and colon/fields segment
                        //     (items[2] :, items[2] {a,b}:)
                        if (DecodeHelper.hasUnquotedBrackets(key)) {
                            throw new IllegalArgumentException(
                                "Invalid array header syntax at line " + (context.currentLine + 1));
                        }
                    }
                    final String key = line.substring(0, colonIdx).trim();
                    final String value = line.substring(colonIdx + 1).trim();
                    result = KeyDecoder.parseKeyValuePair(key, value, depth, depth == 0, context);
                } else {
                    // Bare scalar value
                    if (context.options.strict() && DecodeHelper.hasUnquotedBrackets(line)) {
                        // Line has brackets but no colon and didn't match KEYED_ARRAY_PATTERN
                        // (e.g. "items[2]{id,name}" missing colon)
                        throw new IllegalArgumentException(
                            "Invalid syntax: unquoted brackets without valid header at line "
                                + (context.currentLine + 1));
                    }
                    result = ObjectDecoder.parseBareScalarValue(line, depth, context);
                }
            }
        }

        // The root form spans the whole document (§5); leftover lines must not be
        // silently discarded.
        DecodeHelper.validateNoTrailingContent(context);
        return result;
    }

    /**
     * Spec §5.1: a full-line comment is a line whose first non-space character
     * is '#'. Only U+0020 spaces may precede it; a tab or any other character
     * disqualifies the line, and '#' anywhere else is data.
     *
     * @param line the stripped line to test
     * @return true if the line is a full-line comment
     */
    private static boolean isCommentLine(final String line) {
        int index = 0;
        while (index < line.length() && line.charAt(index) == ' ') {
            index++;
        }
        return index < line.length() && line.charAt(index) == '#';
    }

    /**
     * Decodes a TOON-formatted string directly to a JSON string using custom
     * options.
     *
     * <p>
     * This is a convenience method that decodes TOON to Java objects and then
     * serializes them to JSON.
     * </p>
     *
     * @param toon    The TOON-formatted string to decode
     * @param options Decoding options (indent, delimiter, strict mode)
     * @return JSON string representation
     * @throws IllegalArgumentException if strict mode is enabled and input is
     *                                  invalid
     */
    public static String decodeToJson(final String toon, final DecodeOptions options) {
        try {
            final Object decoded = decode(toon, options);
            if (decoded == null) {
                return NULL_LITERAL;
            }
            return MAPPER.writeValueAsString(decoded);
        } catch (IllegalArgumentException e) {
            // decode() already threw, or strict-mode structural failure
            // re-throw with wrapping for consistency
            throw new IllegalArgumentException("Failed to convert decoded value to JSON", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert decoded value to JSON: " + e.getMessage(), e);
        }
    }
}
