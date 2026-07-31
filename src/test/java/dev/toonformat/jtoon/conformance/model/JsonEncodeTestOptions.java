package dev.toonformat.jtoon.conformance.model;

public record JsonEncodeTestOptions(
        Integer indentSize,
        String delimiter,
        String lengthMarker,
        String keyFolding,
        Integer flattenDepth) {
}

