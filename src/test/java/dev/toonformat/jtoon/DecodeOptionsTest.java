package dev.toonformat.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DecodeOptions configuration record.
 */
@Tag("unit")
public class DecodeOptionsTest {

    @Nested
    @DisplayName("Default Options")
    class DefaultOptions {

        @Test
        @DisplayName("should have correct default values")
        void testDefaultValues() {
            // Given
            DecodeOptions options = DecodeOptions.DEFAULT;

            // Then
            assertEquals(2, options.indent());
            assertEquals(Delimiter.COMMA, options.delimiter());
            assertTrue(options.strict());
        }

        @Test
        @DisplayName("should create options with no-arg constructor")
        void testNoArgConstructor() {
            // Given
            DecodeOptions options = new DecodeOptions();

            // Then
            assertEquals(2, options.indent());
            assertEquals(Delimiter.COMMA, options.delimiter());
            assertTrue(options.strict());
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("withIndent should create options with custom indent")
        void testWithIndent() {
            // Given
            DecodeOptions options = DecodeOptions.withIndent(4);

            // Then
            assertEquals(4, options.indent());
            assertEquals(Delimiter.COMMA, options.delimiter());
            assertTrue(options.strict());
        }

        @Test
        @DisplayName("withDelimiter should create options with custom delimiter")
        void testWithDelimiter() {
            // Given
            DecodeOptions options = DecodeOptions.withDelimiter(Delimiter.PIPE);

            // Then
            assertEquals(2, options.indent());
            assertEquals(Delimiter.PIPE, options.delimiter());
            assertTrue(options.strict());
        }

        @Test
        @DisplayName("withStrict should create options with custom strict mode")
        void testWithStrict() {
            // Given
            DecodeOptions options = DecodeOptions.withStrict(false);

            // Then
            assertEquals(2, options.indent());
            assertEquals(Delimiter.COMMA, options.delimiter());
            assertFalse(options.strict());
        }
    }

    @Nested
    @DisplayName("Custom Options")
    class CustomOptions {

        @Test
        @DisplayName("should create options with all custom values")
        void testAllCustomValues() {
            // Given
            DecodeOptions options = new DecodeOptions(4, Delimiter.TAB, false, PathExpansion.OFF);

            // Then
            assertEquals(4, options.indent());
            assertEquals(Delimiter.TAB, options.delimiter());
            assertFalse(options.strict());
        }

        @Test
        @DisplayName("should support all delimiter types")
        void testAllDelimiters() {
            // Then
            assertEquals(Delimiter.COMMA, new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF).delimiter());
            assertEquals(Delimiter.TAB, new DecodeOptions(2, Delimiter.TAB, true, PathExpansion.OFF).delimiter());
            assertEquals(Delimiter.PIPE, new DecodeOptions(2, Delimiter.PIPE, true, PathExpansion.OFF).delimiter());
        }
    }

    @Nested
    @DisplayName("Record Behavior")
    class RecordBehavior {

        @Test
        @DisplayName("should be equal when values are equal")
        void testEquality() {
            // Given
            DecodeOptions options1 = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);
            DecodeOptions options2 = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);

            // Then
            assertEquals(options1, options2);
            assertEquals(options1.hashCode(), options2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when values differ")
        void testInequality() {
            // Given
            DecodeOptions options1 = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);
            DecodeOptions options2 = new DecodeOptions(4, Delimiter.COMMA, true, PathExpansion.OFF);
            DecodeOptions options3 = new DecodeOptions(2, Delimiter.PIPE, true, PathExpansion.OFF);
            DecodeOptions options4 = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF);

            // Then
            assertNotEquals(options1, options2);
            assertNotEquals(options1, options3);
            assertNotEquals(options1, options4);
        }

        @Test
        @DisplayName("should have meaningful toString")
        void testToString() {
            // Given
            DecodeOptions options = new DecodeOptions(4, Delimiter.TAB, false, PathExpansion.OFF);

            // When
            String str = options.toString();

            // Then
            assertTrue(str.contains("4"), "ToString should contain indent value: " + str);
            assertTrue(str.contains("TAB") || str.contains("delimiter="), "ToString should contain delimiter: " + str);
            assertTrue(str.contains("false") || str.contains("strict="), "ToString should contain strict value: " + str);
        }
    }
}
