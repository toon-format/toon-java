package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;

import java.util.regex.Matcher;

import static dev.toonformat.jtoon.util.Headers.KEYED_ARRAY_PATTERN;

/**
 * Parser class managing line-by-line parsing state.
 * Maintains the currentLine index and uses recursive descent for nested structures.
 */
public class DecodeParser {

    private final DecodeContext context = new DecodeContext();

    DecodeParser(String toon, DecodeOptions options) {
        this.context.lines = toon.split("\n", -1);
        this.context.options = options;
        this.context.delimiter = options.delimiter().toString();
    }

    /**
     * Parses the current line at root level (depth 0).
     * Routes to appropriate handler-based online content.
     */
    Object parseValue() {
        if (context.currentLine >= context.lines.length) {
            return null;
        }

        String line = context.lines[context.currentLine];
        int depth = DecodeHelper.getDepth(line, context);

        if (depth > 0) {
            return DecodeHelper.handleUnexpectedIndentation(context);
        }

        String content = line.substring(depth * context.options.indent());

        // Handle standalone arrays: [2]:
        if (content.startsWith("[")) {
            return ArrayDecoder.parseArray(content, depth, context);
        }

        // Handle keyed arrays: items[2]{id,name}:
        Matcher keyedArray = KEYED_ARRAY_PATTERN.matcher(content);
        if (keyedArray.matches()) {
            return KeyDecoder.parseKeyedArrayValue(keyedArray, content, depth, context);
        }

        // Handle key-value pairs: name: Ada
        int colonIdx = DecodeHelper.findUnquotedColon(content);
        if (colonIdx > 0) {
            String key = content.substring(0, colonIdx).trim();
            String value = content.substring(colonIdx + 1).trim();
            return KeyDecoder.parseKeyValuePair(key, value, depth, depth == 0, context);
        }

        // Bare scalar value
        return ObjectDecoder.parseBareScalarValue(content, depth, context);
    }
}
