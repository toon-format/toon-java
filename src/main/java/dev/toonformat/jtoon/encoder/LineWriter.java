package dev.toonformat.jtoon.encoder;

import static dev.toonformat.jtoon.util.Constants.SPACE;

/**
 * Line writer that accumulates indented lines for building the final output.
 * Uses StringBuilder for efficient string building.
 */
@SuppressWarnings("PMD.AvoidStringBufferField")
public final class LineWriter {
    private final StringBuilder stringBuilder;
    private final String indentationString;
    private final String[] indentCache;
    private boolean firstLine = true;

    private static final int MAX_INDENT_CACHE = 16;
    private static final int INITIAL_BUFFER_SIZE = 1024;

    /**
     * Creates a LineWriter with the specified indentation size.
     *
     * @param indentSize Number of spaces per indentation level
     */
    public LineWriter(final int indentSize) {
        this.stringBuilder = new StringBuilder(INITIAL_BUFFER_SIZE);
        this.indentationString = SPACE.repeat(indentSize);
        this.indentCache = new String[MAX_INDENT_CACHE];

        if (indentSize > 0) {
            final StringBuilder indent = new StringBuilder();
            for (int i = 0; i < MAX_INDENT_CACHE; i++) {
                indentCache[i] = indent.toString();
                indent.append(indentationString);
            }
        }
    }

    /**
     * Adds a line with the specified depth and content.
     *
     * @param depth   Indentation depth (0 = no indentation)
     * @param content Line content to add
     */
    public void push(final int depth, final String content) {
        if (!firstLine) {
            stringBuilder.append('\n');
        }
        firstLine = false;

        if (depth > 0) {
            if (depth < indentCache.length) {
                stringBuilder.append(indentCache[depth]);
            } else {
                stringBuilder.append(String.valueOf(indentationString).repeat(depth));
            }
        }
        stringBuilder.append(content);
    }

    /**
     * Returns the complete output string.
     *
     * @return The complete output string
     */
    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}
