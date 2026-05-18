package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;

/**
 * Deals with the main attributes used to decode TOON to JSON format.
 */
public class DecodeContext {

    /**
     * Lines of the TOON file.
     */
    protected String[] lines;
    /**
     * Options used to decode the TOON file.
     */
    protected DecodeOptions options;
    /**
     * Delimiter used to split array elements.
     */
    protected Delimiter delimiter;
    /**
     * Current line being decoded.
     */
    protected int currentLine;

    private int currentDepth;

    /**
     * Default constructor.
     */
    public DecodeContext() {
        this.currentDepth = 0;
    }

    void incrementDepth() {
        currentDepth++;
        if (currentDepth > options.maxDepth()) {
            throw new IllegalArgumentException(
                "Maximum nesting depth exceeded: " + options.maxDepth());
        }
    }

    void decrementDepth() {
        currentDepth--;
    }

}
