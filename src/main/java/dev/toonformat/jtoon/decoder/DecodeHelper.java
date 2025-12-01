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

        // Validate indentation (including tabs) in strict mode
        // Check for tabs first before any other processing
        if (context.options.strict() && !line.isEmpty() && line.charAt(0) == '\t') {
            throw new IllegalArgumentException(
                String.format("Tab character used in indentation at line %d", context.currentLine + 1));
        }

        if (context.options.strict()) {
            validateIndentation(line, context);
        }

        int depth;
        int leadingSpaces = getLeadingSpaces(line, context);

        //never div to zero
        if (context.options.indent() == 0) {
            return leadingSpaces;
        }

        // Calculate depth based on indent size
        depth = leadingSpaces / context.options.indent();

        return depth;
    }

    /**
     * Get the amount of leading spaces
     *
     * @param line    the line string to parse
     * @param context decode an object to deal with lines, delimiter, and options
     * @return the amount of leading spaces of the given line
     */
    private static int getLeadingSpaces(String line, DecodeContext context) {
        int leadingSpaces = 0;

        // Count leading spaces
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') {
                leadingSpaces++;
            } else {
                break;
            }
        }
        int indentSize = context.options.indent();

        // In strict mode, check if it's an exact multiple
        if (context.options.strict() && leadingSpaces > 0 && leadingSpaces % indentSize != 0) {
            throw new IllegalArgumentException(
                String.format("Non-multiple indentation: %d spaces with indent=%d at line %d",
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
     * Validates indentation in strict mode.
     * Checks for tabs, mixed tabs/spaces, and non-multiple indentation.
     *
     * @param line    the line string to parse
     * @param context decode an object to deal with lines, delimiter, and options
     */
    private static void validateIndentation(String line, DecodeContext context) {
        if (line.trim().isEmpty()) {
            // Blank lines are allowed (handled separately)
            return;
        }

        int indentSize = context.options.indent();
        int leadingSpaces = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\t') {
                throw new IllegalArgumentException(
                    String.format("Tab character used in indentation at line %d", context.currentLine + 1));
            } else if (c == ' ') {
                leadingSpaces++;
            } else {
                // Reached non-whitespace
                break;
            }
        }

        // Check for non-multiple indentation (only if there's actual content)
        if (leadingSpaces > 0 && leadingSpaces % indentSize != 0) {
            throw new IllegalArgumentException(
                String.format("Non-multiple indentation: %d spaces with indent=%d at line %d",
                    leadingSpaces, indentSize, context.currentLine + 1));
        }
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

    /**
     * Handles unexpected indentation at root level.
     *
     * @param context decode an object to deal with lines, delimiter, and options
     * @return null if in non-strict mode, otherwise throws an exception
     * @throws IllegalArgumentException in case there's an unexpected indentation
     */
    protected static Object handleUnexpectedIndentation(DecodeContext context) {
        if (context.options.strict()) {
            throw new IllegalArgumentException("Unexpected indentation at line " + context.currentLine);
        }
        return null;
    }
}
