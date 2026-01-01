package dev.toonformat.jtoon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncodeOptionsTest {

    @Test
    void givenDefaultConstructor_whenCreateInstance_thenUsesDefaultValues() {
        // Given

        // When
        EncodeOptions opts = new EncodeOptions();

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertFalse(opts.lengthMarker());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenDefaultStaticInstance_whenAccess_thenValuesMatchDefaultConstructor() {
        // Given
        EncodeOptions opts = EncodeOptions.DEFAULT;

        // When
        // (direct access)

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertFalse(opts.lengthMarker());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenCustomIndent_whenUsingWithIndent_thenOnlyIndentIsModified() {
        // Given
        int indent = 4;

        // When
        EncodeOptions opts = EncodeOptions.withIndent(indent);

        // Then
        assertEquals(4, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertFalse(opts.lengthMarker());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenCustomDelimiter_whenUsingWithDelimiter_thenOnlyDelimiterIsModified() {
        // Given
        Delimiter delimiter = Delimiter.TAB;

        // When
        EncodeOptions opts = EncodeOptions.withDelimiter(delimiter);

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.TAB, opts.delimiter());
        assertFalse(opts.lengthMarker());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenLengthMarkerFlag_whenUsingWithLengthMarker_thenOnlyLengthMarkerIsModified() {
        // Given
        boolean marker = true;

        // When
        EncodeOptions opts = EncodeOptions.withLengthMarker(marker);

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertTrue(opts.lengthMarker());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenFlattenFlag_whenUsingWithFlatten_thenOnlyFlattenIsModified() {
        // Given
        boolean flatten = true;

        // When
        EncodeOptions opts = EncodeOptions.withFlatten(flatten);

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertFalse(opts.lengthMarker());
        assertEquals(KeyFolding.SAFE, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenNegativeFlattenFlag_whenUsingWithFlatten_thenOnlyFlattenIsModified() {
        // Given
        boolean flatten = false;

        // When
        EncodeOptions opts = EncodeOptions.withFlatten(flatten);

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertFalse(opts.lengthMarker());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenFlattenDepth_whenUsingWithFlattenDepth_thenFlattenDepthIsSetAndFlattenIsTrue() {
        // Given
        int flattenDepth = 3;

        // When
        EncodeOptions opts = EncodeOptions.withFlattenDepth(flattenDepth);

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertFalse(opts.lengthMarker());
        assertEquals(KeyFolding.SAFE, opts.flatten());
        assertEquals(3, opts.flattenDepth());
    }
}
