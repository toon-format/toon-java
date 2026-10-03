package dev.toonformat.jtoon;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EncodeOptionsTest {

    @Test
    void givenDefaultConstructor_whenCreateInstance_thenUsesDefaultValues() {
        // Given

        // When
        final EncodeOptions opts = new EncodeOptions();

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenDefaultStaticInstance_whenAccess_thenValuesMatchDefaultConstructor() {
        // Given
        final EncodeOptions opts = EncodeOptions.DEFAULT;

        // When
        // (direct access)

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenCustomIndent_whenUsingWithIndent_thenOnlyIndentIsModified() {
        // Given
        final int indent = 4;

        // When
        final EncodeOptions opts = EncodeOptions.withIndent(indent);

        // Then
        assertEquals(indent, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenCustomDelimiter_whenUsingWithDelimiter_thenOnlyDelimiterIsModified() {
        // Given
        final Delimiter delimiter = Delimiter.TAB;

        // When
        final EncodeOptions opts = EncodeOptions.withDelimiter(delimiter);

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.TAB, opts.delimiter());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenFlattenFlag_whenUsingWithFlatten_thenOnlyFlattenIsModified() {
        // Given
        final boolean flatten = true;

        // When
        final EncodeOptions opts = EncodeOptions.withFlatten(flatten);

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertEquals(KeyFolding.SAFE, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenNegativeFlattenFlag_whenUsingWithFlatten_thenOnlyFlattenIsModified() {
        // Given
        final boolean flatten = false;

        // When
        final EncodeOptions opts = EncodeOptions.withFlatten(flatten);

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertEquals(KeyFolding.OFF, opts.flatten());
        assertEquals(Integer.MAX_VALUE, opts.flattenDepth());
    }

    @Test
    void givenFlattenDepth_whenUsingWithFlattenDepth_thenFlattenDepthIsSetAndFlattenIsTrue() {
        // Given
        final int flattenDepth = 3;

        // When
        final EncodeOptions opts = EncodeOptions.withFlattenDepth(flattenDepth);

        // Then
        assertEquals(2, opts.indent());
        assertEquals(Delimiter.COMMA, opts.delimiter());
        assertEquals(KeyFolding.SAFE, opts.flatten());
        assertEquals(flattenDepth, opts.flattenDepth());
    }
}
