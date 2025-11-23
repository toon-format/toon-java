package dev.toonformat.jtoon.encoder;


import dev.toonformat.jtoon.EncodeOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.toonformat.jtoon.util.Constants.COLON;
import static dev.toonformat.jtoon.util.Constants.SPACE;

/**
 * Handles encoding of JSON objects to TOON format.
 * Recursively encodes nested objects and delegates arrays to ArrayEncoder.
 */
public final class ObjectEncoder {

    private ObjectEncoder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encodes an ObjectNode to TOON format.
     *
     * @param value           The ObjectNode to encode
     * @param writer          LineWriter for accumulating output
     * @param depth           Current indentation depth
     * @param options         Encoding options
     * @param rootLiteralKeys optional set of dotted keys at the root level to avoid collisions
     * @param pathPrefix      optional parent dotted path (for absolute collision checks)
     * @param remainingDepth  optional override for the remaining depth
     * @param blockedKeys     contains only keys that have undergone a successful flattening
     */
    public static void encodeObject(ObjectNode value, LineWriter writer, int depth, EncodeOptions options, Set<String> rootLiteralKeys, String pathPrefix, Integer remainingDepth, Set<String> blockedKeys) {
        List<Map.Entry<String, JsonNode>> fields = value.properties().stream().toList();

        // At root level (depth 0), collect all literal dotted keys for collision checking
        if (depth == 0 && rootLiteralKeys != null) {
            rootLiteralKeys.clear();
            fields.stream()
                    .filter(e -> e.getKey().contains("."))
                    .map(Map.Entry::getKey)
                    .forEach(rootLiteralKeys::add);
        }
        int effectiveFlattenDepth = remainingDepth != null ? remainingDepth : options.flattenDepth();

        //the siblings collision do not need the absolute path
        Set<String> siblings = fields.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (Map.Entry<String, JsonNode> entry : fields) {
            encodeKeyValuePair(entry.getKey(), entry.getValue(), writer, depth, options, siblings, rootLiteralKeys, pathPrefix, effectiveFlattenDepth, blockedKeys);
        }
    }

    /**
     * Encodes a key-value pair in an object.
     *
     * @param key             the key name
     * @param value           the value to encode
     * @param writer          the LineWriter for accumulating output
     * @param depth           the current indentation depth
     * @param options         encoding options
     * @param siblings        set of sibling keys for collision detection
     * @param rootLiteralKeys optional set of dotted keys at the root level to avoid collisions
     * @param pathPrefix      optional parent dotted path (for absolute collision checks)
     * @param flattenDepth    optional override for depth limit
     * @param blockedKeys     contains only keys that have undergone a successful flattening
     */
    public static void encodeKeyValuePair(String key,
                                          JsonNode value,
                                          LineWriter writer,
                                          int depth,
                                          EncodeOptions options,
                                          Set<String> siblings,
                                          Set<String> rootLiteralKeys,
                                          String pathPrefix,
                                          Integer flattenDepth,
                                          Set<String> blockedKeys
    ) {
        String encodedKey = PrimitiveEncoder.encodeKey(key);
        String currentPath = pathPrefix != null ? pathPrefix + "." + key : key;
        int effectiveFlattenDepth = flattenDepth != null && flattenDepth > 0 ? flattenDepth : options.flattenDepth();
        int remainingDepth = effectiveFlattenDepth - depth;

        // Attempt key folding when enabled
        if (options.flatten()
                && !siblings.isEmpty()
                && remainingDepth > 0
                && blockedKeys != null
                && !blockedKeys.contains(key)) {
            Flatten.FoldResult foldResult = Flatten.tryFoldKeyChain(key, value, siblings, rootLiteralKeys, pathPrefix, remainingDepth);
            if (foldResult != null) {
                options = flatten(key, foldResult, writer, depth, options, rootLiteralKeys, pathPrefix, blockedKeys, remainingDepth);
                if (options == null) {
                    return;
                }
            }
        }

        if (value.isValueNode()) {
            writer.push(depth, encodedKey + COLON + SPACE + PrimitiveEncoder.encodePrimitive(value, options.delimiter().getValue()));
        } else if (value.isArray()) {
            ArrayEncoder.encodeArray(key, (ArrayNode) value, writer, depth, options);
        } else if (value.isObject()) {
            ObjectNode objValue = (ObjectNode) value;
            writer.push(depth, encodedKey + COLON);
            if (!objValue.isEmpty()) {
                encodeObject(objValue, writer, depth + 1, options, rootLiteralKeys, currentPath, effectiveFlattenDepth, blockedKeys);
            }
        }
    }

    /**
     * Extract to flatten methode for better maintenance.
     *
     * @param key             the key name
     * @param foldResult      the result of the folding
     * @param writer          the LineWriter for accumulating output
     * @param depth           the current indentation depth
     * @param options         encoding options
     * @param rootLiteralKeys optional set of dotted keys at the root level to avoid collisions
     * @param pathPrefix      optional parent dotted path (for absolute collision checks)
     * @param blockedKeys     contains only keys that have undergone a successful flattening
     * @param remainingDepth  the depth that remind to the limit
     * @return EncodeOptions changes for Case 2
     */
    private static EncodeOptions flatten(String key, Flatten.FoldResult foldResult, LineWriter writer, int depth, EncodeOptions options,Set<String> rootLiteralKeys, String pathPrefix, Set<String> blockedKeys, int remainingDepth) {
        String foldedKey = foldResult.foldedKey();

        // prevent second folding pass
        blockedKeys.add(key);
        blockedKeys.add(foldedKey);

        String encodedFoldedKey = PrimitiveEncoder.encodeKey(foldedKey);
        JsonNode remainder = foldResult.remainder();

        // Case 1: Fully folded to a leaf value
        if (remainder == null) {
            handleFullyFoldedLeaf(foldResult, writer, depth, options, encodedFoldedKey);
            return null;
        }

        // Case 2: Partially folded with a tail object
        if (remainder.isObject()) {
            writer.push(depth, indentedLine(depth, encodedFoldedKey + ":", options.indent()));

            String foldedPath = pathPrefix != null ? String.join(".", pathPrefix, foldedKey) : foldedKey;
            int newRemainingDepth = remainingDepth - foldResult.segmentCount();

            if (newRemainingDepth <= 0) {
                // Pass "-1" if remainingDepth is exhausted and set the encoding in the option to false.
                // to encode normally without flattening
                newRemainingDepth = -1;
                options = new EncodeOptions(options.indent(), options.delimiter(), options.lengthMarker(), false, options.flattenDepth());
            }

            encodeObject((ObjectNode) remainder, writer, depth + 1, options, rootLiteralKeys, foldedPath, newRemainingDepth, blockedKeys);
            return null;
        }

        return options;
    }

    private static void handleFullyFoldedLeaf(Flatten.FoldResult foldResult, LineWriter writer, int depth, EncodeOptions options, String encodedFoldedKey) {
        JsonNode leaf = foldResult.leafValue();

        // Primitive
        if (leaf.isValueNode()) {
            writer.push(depth,
                    indentedLine(depth,
                            encodedFoldedKey + ": " +
                                    PrimitiveEncoder.encodePrimitive(leaf, options.delimiter().getValue()),
                            options.indent()));
            return;
        }

        // Array
        if (leaf.isArray()) {
            ArrayEncoder.encodeArray(foldResult.foldedKey(), (ArrayNode) leaf, writer, depth, options);
            return;
        }

        // Object
        if (leaf.isObject()) {
            writer.push(depth, indentedLine(depth, encodedFoldedKey + ":", options.indent()));
            if (!leaf.isEmpty()) {
                encodeObject((ObjectNode) leaf, writer, depth + 1, options,
                        null, null, null, null);
            }
        }
    }

    private static String indentedLine(int depth, String content, int indentSize) {
        return "%s%s".formatted(" ".repeat(indentSize * depth), content);
    }
}
