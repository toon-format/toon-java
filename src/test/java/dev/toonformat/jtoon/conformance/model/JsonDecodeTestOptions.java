package dev.toonformat.jtoon.conformance.model;

public record JsonDecodeTestOptions(
        Integer indentSize,
        String delimiter,
        String lengthMarker,
        Boolean strict,
        String expandPaths) {
}

