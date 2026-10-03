package dev.toonformat.jtoon.conformance.model;

public record JsonDecodeTestOptions(
        Integer indentSize,
        String delimiter,
        Boolean strict,
        String expandPaths) {
}

