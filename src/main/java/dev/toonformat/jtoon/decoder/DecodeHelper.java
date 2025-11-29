package dev.toonformat.jtoon.decoder;

import java.util.List;
import java.util.Map;

public class DecodeHelper {

    private DecodeHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculates indentation depth (nesting level) of a line.
     * Counts leading spaces in multiples of the configured indent size.
     * In strict mode, validates indentation (no tabs, proper multiples).
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
        int indentSize = context.options.indent();
        int leadingSpaces = 0;

        // Count leading spaces
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') {
                leadingSpaces++;
            } else {
                break;
            }
        }

        // Calculate depth based on indent size
        depth = leadingSpaces / indentSize;

        // In strict mode, check if it's an exact multiple
        if (context.options.strict() && leadingSpaces > 0
            && leadingSpaces % indentSize != 0) {
            throw new IllegalArgumentException(
                String.format("Non-multiple indentation: %d spaces with indent=%d at line %d",
                    leadingSpaces, indentSize, context.currentLine + 1));
        }

        return depth;
    }

    /**
     * Checks if a line is blank (empty or only whitespace).
     */
    protected static boolean isBlankLine(String line) {
        return line.trim().isEmpty();
    }

    /**
     * Validates indentation in strict mode.
     * Checks for tabs, mixed tabs/spaces, and non-multiple indentation.
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
     */
    protected static int findNextNonBlankLine(int startIndex, DecodeContext context) {
        int index = startIndex;
        while (index < context.lines.length && isBlankLine(context.lines[index])) {
            index++;
        }
        return index;
    }

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
     */
    protected static void checkPathExpansionConflict(Map<String, Object> map, String key, Object value, DecodeContext context) {
        if (!context.options.strict()) {
            return;
        }

        Object existing = map.get(key);
        DecodeHelper.checkFinalValueConflict(key, existing, value, context);
    }

    /**
     * Finds the depth of the next non-blank line, skipping blank lines.
     *
     * @return the depth of the next non-blank line, or null if none exists
     */
    protected static Integer findNextNonBlankLineDepth(DecodeContext context) {
        int nextLineIdx = context.currentLine;
        while (nextLineIdx < context.lines.length && DecodeHelper.isBlankLine(context.lines[nextLineIdx])) {
            nextLineIdx++;
        }

        if (nextLineIdx >= context.lines.length) {
            return null;
        }

        return DecodeHelper.getDepth(context.lines[nextLineIdx], context);
    }

    /**
     * Validates that there are no multiple primitives at root level in strict mode.
     */
    protected static void validateNoMultiplePrimitivesAtRoot(DecodeContext context) {
        int lineIndex = context.currentLine;
        while (lineIndex < context.lines.length && DecodeHelper.isBlankLine(context.lines[lineIndex])) {
            lineIndex++;
        }
        if (lineIndex < context.lines.length) {
            int nextDepth = DecodeHelper.getDepth(context.lines[lineIndex], context);
            if (nextDepth == 0) {
                throw new IllegalArgumentException(
                    "Multiple primitives at root depth in strict mode at line " + (lineIndex + 1));
            }
        }
    }

    /**
     * Handles unexpected indentation at root level.
     */
    protected static Object handleUnexpectedIndentation(DecodeContext context) {
        if (context.options.strict()) {
            throw new IllegalArgumentException("Unexpected indentation at line " + context.currentLine);
        }
        return null;
    }
}
