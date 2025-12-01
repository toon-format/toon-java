package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;

/**
 * Deals with the main attributes used to decode TOON to JSON format
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
    protected String delimiter;
    /**
     * Current line being decoded.
     */
    protected int currentLine = 0;

    /**
     * Default constructor
     */
    public DecodeContext() {}
}
