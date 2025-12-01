package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;

/**
 * Deals with the main attributes used to decode TOON to JSON format
 */
public class DecodeContext {

    protected String[] lines;
    protected DecodeOptions options;
    protected String delimiter;
    protected int currentLine = 0;

    public DecodeContext () {}
}
