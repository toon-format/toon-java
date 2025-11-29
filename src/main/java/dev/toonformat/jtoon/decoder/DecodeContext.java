package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;

public class DecodeContext {

    protected String[] lines;
    protected DecodeOptions options;
    protected String delimiter;
    protected int currentLine = 0;

    public DecodeContext () {}
}
