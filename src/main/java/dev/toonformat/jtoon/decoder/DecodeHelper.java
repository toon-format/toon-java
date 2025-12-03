package dev.toonformat.jtoon.decoder;

import java.util.List;
import java.util.Map;

/**
 * Handles indentation, depth, conflicts, and validation for other decode classes.
 */
public class DecodeHelper {

    private DecodeHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculates indentation depth (nesting level) of a line.
     * Counts leading spaces in multiples of the configured indent size.
     * In strict mode, validates indentation (no tabs, proper multiples).
     *
     * @param line    the line string to parse
     * @param context decode an object to deal with lines, delimiter, and options
     * @return the depth of a line
     */
    public static int getDepth(String line, DecodeContext context) {
        // Blank lines (including lines with only spaces) have depth 0
        if (isBlankLine(line)) {
            return 0;
        }
        return computeLeadingSpaces(line, context) / Math.max(1, context.options.indent());
    }

    /**
     * Computes leading spaces, validates indentation in strict mode,
     * and rejects tabs. Single scan for all indentation logic.
     *
     * @param line    the line string to parse
     * @param context decode object in order to deal with lines, delimiter and options
     * @return amount of leading spaces
     */
    private static int computeLeadingSpaces(String line, DecodeContext context) {
        int indentSize = context.options.indent();
        int leadingSpaces = 0;

        int i = 0;
        int lengthOfLine = line.length();
        while (i < lengthOfLine) {
            char c = line.charAt(i);
            if (c == ' ') {
                leadingSpaces++;
            } else if (c == '\t') {
                if (context.options.strict()) {
                    throw new IllegalArgumentException(
                        "Tab character used in indentation at line " + (context.currentLine + 1));
                }
                // In non-strict mode treat tab as non-indent and stop.
                break;
            } else {
                break; // reached content
            }
            i++;
        }

        if (context.options.strict() && leadingSpaces > 0 && indentSize > 0 && leadingSpaces % indentSize != 0) {
            throw new IllegalArgumentException(
                String.format("Non-multiple indentation: %d leadingSpaces with indent=%d at line %d",
                    leadingSpaces, indentSize, context.currentLine + 1));
        }

        return leadingSpaces;
    }


    /**
     * Checks if a line is blank (empty or only whitespace).
     *
     * @param line the line string to parse
     * @return true or false depending on if the line is blank or not
     */
    protected static boolean isBlankLine(String line) {
        return line.trim().isEmpty();
    }

    /**
     * Finds the index of the first unquoted colon in a line.
     * Critical for handling quoted keys like "order:id": value.
     *
     * @param content the content string to parse
     * @return the unquoted colon
     */
    protected static int findUnquotedColon(String content) {
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ':' && !inQuotes) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Finds the next non-blank line starting from the given index.
     *
     * @param startIndex given index
     * @param context    decode an object to deal with lines, delimiter, and options
     * @return index aiming for the next non-blank line
     */
    protected static int findNextNonBlankLine(int startIndex, DecodeContext context) {
        int index = startIndex;
        while (index < context.lines.length && isBlankLine(context.lines[index])) {
            index++;
        }
        return index;
    }

    /**
     * Finds the next non-blank line starting from the given index.
     *
     * @param finalSegment final segment
     * @param existing     existing
     * @param value        value present in a map
     * @param context      decode an object to deal with lines, delimiter, and options
     * @throws IllegalArgumentException in case there's a expansion conflict
     */
    protected static void checkFinalValueConflict(String finalSegment, Object existing, Object value, DecodeContext context) {
        if (existing != null && context.options.strict()) {
            // Check for conflicts in strict mode
            if (existing instanceof Map && !(value instanceof Map)) {
                throw new IllegalArgumentException(
                    String.format("Path expansion conflict: %s is object, cannot set to %s",
                        finalSegment, value.getClass().getSimpleName()));
            }
            if (existing instanceof List && !(value instanceof List)) {
                throw new IllegalArgumentException(
                    String.format("Path expansion conflict: %s is array, cannot set to %s",
                        finalSegment, value.getClass().getSimpleName()));
            }
        }
    }

    /**
     * Checks for path expansion conflicts when setting a non-expanded key.
     * In strict mode, throws if the key conflicts with an existing expanded path.
     *
     * @param map     map
     * @param key     present the key in the map
     * @param value   present value in a map
     * @param context decode an object to deal with lines, delimiter, and options
     */
    protected static void checkPathExpansionConflict(Map<String, Object> map, String key, Object value, DecodeContext context) {
        if (!context.options.strict()) {
            return;
        }

        Object existing = map.get(key);
        checkFinalValueConflict(key, existing, value, context);
    }

    /**
     * Finds the depth of the next non-blank line, skipping blank lines.
     *
     * @param context decode an object to deal with lines, delimiter, and options
     * @return the depth of the next non-blank line, or null if none exists
     */
    protected static Integer findNextNonBlankLineDepth(DecodeContext context) {
        int nextLineIdx = context.currentLine;
        while (nextLineIdx < context.lines.length && isBlankLine(context.lines[nextLineIdx])) {
            nextLineIdx++;
        }

        if (nextLineIdx >= context.lines.length) {
            return null;
        }

        return getDepth(context.lines[nextLineIdx], context);
    }

    /**
     * Validates that there are no multiple primitives at root level in strict mode.
     *
     * @param context decode an object to deal with lines, delimiter, and options
     * @throws IllegalArgumentException in case the next depth is equal to 0
     */
    protected static void validateNoMultiplePrimitivesAtRoot(DecodeContext context) {
        int lineIndex = context.currentLine;
        while (lineIndex < context.lines.length && isBlankLine(context.lines[lineIndex])) {
            lineIndex++;
        }
        if (lineIndex < context.lines.length) {
            int nextDepth = getDepth(context.lines[lineIndex], context);
            if (nextDepth == 0) {
                throw new IllegalArgumentException(
                    "Multiple primitives at root depth in strict mode at line " + (lineIndex + 1));
            }
        }
    }

}
