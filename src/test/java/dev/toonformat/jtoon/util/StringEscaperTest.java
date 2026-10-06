package dev.toonformat.jtoon.util;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for StringEscaper utility class.
 * Tests string escaping logic for special characters in TOON format.
 */
@Tag("unit")
public class StringEscaperTest {

    @Nested
    @DisplayName("Basic Escaping")
    class BasicEscaping {

        static Stream<Arguments> basicEscapingCases() {
            return Stream.of(
                Arguments.of("backslashes", "path\\to\\file", "path\\\\to\\\\file"),
                Arguments.of("double quotes", "He said \"hello\"", "He said \\\"hello\\\""),
                Arguments.of("newlines", "line1\nline2", "line1\\nline2"),
                Arguments.of("carriage returns", "line1\rline2", "line1\\rline2"),
                Arguments.of("tabs", "col1\tcol2", "col1\\tcol2"));
        }

        @ParameterizedTest(name = "should escape {0}")
        @MethodSource("basicEscapingCases")
        @DisplayName("should escape basic special characters")
        void testBasicEscaping(final String description, final String input, final String expected) {
            // Then
            assertEquals(expected, StringEscaper.escape(input));
        }
    }

    @Nested
    @DisplayName("Combined Escaping")
    class CombinedEscaping {

        static Stream<Arguments> combinedEscapingCases() {
            return Stream.of(
                Arguments.of("multiple special characters", "He said \"test\\path\"\nNext line",
                    "He said \\\"test\\\\path\\\"\\nNext line"),
                Arguments.of("all control characters together", "text\n\r\t", "text\\n\\r\\t"),
                Arguments.of("backslash before quote", "\\\"", "\\\\\\\""));
        }

        @ParameterizedTest(name = "should escape {0}")
        @MethodSource("combinedEscapingCases")
        @DisplayName("should escape combined special characters")
        void testCombinedEscaping(final String description, final String input, final String expected) {
            // Then
            assertEquals(expected, StringEscaper.escape(input));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should return empty string for empty input")
        void testEmptyString() {
            // Then
            assertEquals("", StringEscaper.escape(""));
        }

        @Test
        @DisplayName("should return null for null input")
        void testNullInput() {
            assertNull(StringEscaper.escape(null));
        }

        @ParameterizedTest
        @DisplayName("should not modify strings without special characters")
        @ValueSource(strings = {
            "hello world",
            "Hello World 123 @#$%^&*()_+-=[]{}|;:',.<>?/",
            "Hello 世界 🌍"
        })
        void testStringsWithoutSpecialCharacters(final String input) {
            // Then
            assertEquals(input, StringEscaper.escape(input));
        }

        @Test
        @DisplayName("should handle consecutive backslashes")
        void testConsecutiveBackslashes() {
            // Given
            final String input = "\\\\\\";
            final String expected = "\\\\\\\\\\\\";

            // Then
            assertEquals(expected, StringEscaper.escape(input));
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenarios {

        static Stream<Arguments> realWorldScenarios() {
            return Stream.of(
                Arguments.of("JSON string", "{\"key\": \"value\"}", "{\\\"key\\\": \\\"value\\\"}"),
                Arguments.of("Windows file path", "C:\\Users\\Documents\\file.txt",
                    "C:\\\\Users\\\\Documents\\\\file.txt"),
                Arguments.of("multi-line text", "Line 1\nLine 2\nLine 3", "Line 1\\nLine 2\\nLine 3"),
                Arguments.of("SQL query", "SELECT * FROM users WHERE name = \"John\"",
                    "SELECT * FROM users WHERE name = \\\"John\\\""),
                Arguments.of("regex pattern", "\\d+\\.\\d+", "\\\\d+\\\\.\\\\d+"));
        }

        @ParameterizedTest(name = "should escape {0}")
        @MethodSource("realWorldScenarios")
        @DisplayName("should escape real-world scenarios")
        void testRealWorldScenarios(final String scenario, final String input, final String expected) {
            // Then
            assertEquals(expected, StringEscaper.escape(input));
        }
    }

    @Nested
    @DisplayName("Basic Unescaping")
    class BasicUnescaping {

        static Stream<Arguments> basicUnescapingCases() {
            return Stream.of(
                Arguments.of("backslashes", "path\\\\to\\\\file", "path\\to\\file"),
                Arguments.of("double quotes", "He said \\\"hello\\\"", "He said \"hello\""),
                Arguments.of("newlines", "line1\\nline2", "line1\nline2"),
                Arguments.of("carriage returns", "line1\\rline2", "line1\rline2"),
                Arguments.of("tabs", "col1\\tcol2", "col1\tcol2"));
        }

        @ParameterizedTest(name = "should unescape {0}")
        @MethodSource("basicUnescapingCases")
        @DisplayName("should unescape basic special characters")
        void testBasicUnescaping(final String description, final String input, final String expected) {
            // Then
            assertEquals(expected, StringEscaper.unescape("\"" + input + "\""));
        }
    }

    @Nested
    @DisplayName("Quote Removal")
    class QuoteRemoval {

        @Test
        @DisplayName("should remove surrounding quotes")
        void testQuoteRemoval() {
            // Then
            assertEquals("hello", StringEscaper.unescape("\"hello\""));
        }

        @Test
        @DisplayName("should handle quotes with escaped content")
        void testQuotedEscapedContent() {
            // Then
            assertEquals("hello\nworld", StringEscaper.unescape("\"hello\\nworld\""));
        }

        @Test
        @DisplayName("should not remove quotes if not surrounding")
        void testNonSurroundingQuotes() {
            // Then
            assertEquals("hello\"world", StringEscaper.unescape("hello\"world"));
        }

        @Test
        @DisplayName("should handle empty quoted string")
        void testEmptyQuotedString() {
            // Then
            assertEquals("", StringEscaper.unescape("\"\""));
        }

        @Test
        @DisplayName("should not unquote when string starts with but does not end with quote")
        void testUnmatchedOpeningQuote() {
            assertEquals("\"unclosed", StringEscaper.unescape("\"unclosed"));
        }
    }

    @Nested
    @DisplayName("Round-Trip Escaping")
    class RoundTripEscaping {

        static Stream<String> roundTripCases() {
            return Stream.of(
                "simple text",
                "path\\to\\file",
                "He said \"hello\"",
                "line1\nline2\nline3",
                "col1\tcol2\tcol3",
                "C:\\Users\\Documents",
                "text\n\r\t\"\\"
            );
        }

        @ParameterizedTest
        @DisplayName("should preserve content through escape/unescape cycle")
        @MethodSource("roundTripCases")
        void testRoundTrip(final String original) {
            // Given
            final String escaped = StringEscaper.escape(original);
            final String unescaped = StringEscaper.unescape("\"" + escaped + "\"");

            // Then
            assertEquals(original, unescaped);
        }
    }

    @Nested
    @DisplayName("Unescape Edge Cases")
    class UnescapeEdgeCases {

        @Test
        @DisplayName("should handle null input")
        void testNullInput() {
            // Then
            assertNull(StringEscaper.unescape(null));
        }

        @Test
        @DisplayName("should handle empty string")
        void testEmptyString() {
            // Then
            assertEquals("", StringEscaper.unescape(""));
        }

        @Test
        @DisplayName("should handle single character")
        void testSingleCharacter() {
            // Then
            assertEquals("a", StringEscaper.unescape("a"));
        }

        @Test
        @DisplayName("should handle strings without escape sequences")
        void testNoEscapeSequences() {
            // Then
            assertEquals("hello world", StringEscaper.unescape("hello world"));
        }

        @Test
        @DisplayName("should reject invalid escape sequences")
        void testUnknownEscapeSequences() {
            assertThrows(IllegalArgumentException.class, () -> StringEscaper.unescape("\"\\ax\""));
        }

        @Test
        void unquotesValueWhenStartsAndEndsWithQuote() {
            // Then
            assertEquals("abc", StringEscaper.unescape("\"abc\""));
        }

        @Test
        void unescapesBackslashSequences() {
            // Then
            assertEquals("a\"b", StringEscaper.unescape("\"a\\\"b\""));
        }

        @Test
        void unescapesMultipleCharacters() {
            // Then
            assertEquals("a\nb\tc", StringEscaper.unescape("\"a\\nb\\tc\""));
        }

        @Test
        void handlesTrailingBackslashCorrectly() {
            // Then
            // trailing \ will set escaped=true but there is no next char → nothing appended
            assertEquals("abc", StringEscaper.unescape("\"abc\\\""));
        }

        @Test
        void handlesDoubleBackslashCorrectly() {
            // Then
            assertEquals("a\\b", StringEscaper.unescape("\"a\\\\b\""));
        }
    }

    @Nested
    @DisplayName("Control Character Escaping")
    class ControlCharacterEscaping {

        static Stream<Arguments> controlCharCases() {
            return Stream.of(
                Arguments.of("U+0000 null", "\0", "\\u0000"),
                Arguments.of("U+0004 EOT", "\004", "\\u0004"),
                Arguments.of("U+000F shift-in", "\017", "\\u000f"),
                Arguments.of("U+001B escape", "\033", "\\u001b"),
                Arguments.of("U+001F unit separator", "\037", "\\u001f"),
                Arguments.of("U+0001 in middle", "a\001b", "a\\u0001b"));
        }

        @ParameterizedTest(name = "should escape {0}")
        @MethodSource("controlCharCases")
        @DisplayName("should escape control characters via \\uXXXX")
        void testControlChars(final String description, final String input, final String expected) {
            assertEquals(expected, StringEscaper.escape(input));
        }

        @Test
        @DisplayName("should NOT escape space (U+0020)")
        void testSpaceNotEscaped() {
            assertEquals("a b", StringEscaper.escape("a b"));
        }
    }

    @Nested
    @DisplayName("validateString - Surrogate Pairs")
    class ValidateStringSurrogates {

        @Test
        @DisplayName("should reject surrogate pair")
        void surrogatePair() {
            final String input = "\"a\\uD800\\uDC00b\"";
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> StringEscaper.validateString(input));
            assertTrue(ex.getMessage().contains("surrogate"));
        }

        @Test
        @DisplayName("should reject lone low surrogate")
        void loneLowSurrogate() {
            final String input = "\"a\\uDC00b\"";
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> StringEscaper.validateString(input));
            assertTrue(ex.getMessage().contains("surrogate"));
        }

        @Test
        @DisplayName("should reject lone high surrogate")
        void loneHighSurrogate() {
            final String input = "\"a\\uD800b\"";
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> StringEscaper.validateString(input));
            assertTrue(ex.getMessage().contains("surrogate"));
        }

        @Test
        @DisplayName("should reject invalid hex in \\u escape")
        void invalidUnicodeHex() {
            final String input = "\"a\\u00XXb\"";
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> StringEscaper.validateString(input));
            assertTrue(ex.getMessage().contains("Invalid escape sequence: \\u"));
        }

        @Test
        @DisplayName("should reject truncated \\u escape (fewer than 4 hex chars)")
        void truncatedUnicodeEscape() {
            // \\u00b has only 3 hex chars
            final String input = "\"\\u00b\"";
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> StringEscaper.validateString(input));
            assertEquals("Invalid escape sequence: \\u", ex.getMessage());
        }

        @Test
        @DisplayName("should accept valid standard escapes")
        void validStandardEscapes() {
            assertDoesNotThrow(() -> StringEscaper.validateString("\"\\n\""));
            assertDoesNotThrow(() -> StringEscaper.validateString("\"\\r\""));
            assertDoesNotThrow(() -> StringEscaper.validateString("\"\\t\""));
            assertDoesNotThrow(() -> StringEscaper.validateString("\"\\\\\""));
            assertDoesNotThrow(() -> StringEscaper.validateString("\"\\\"\""));
        }
    }

    @Nested
    @DisplayName("unescape - Unicode Sequences")
    class UnescapeUnicode {

        @Test
        @DisplayName("should unescape \\u0004 to control char")
        void unescapeControlChar() {
            assertEquals("a\004b", StringEscaper.unescape("\"a\\u0004b\""));
        }

        @Test
        @DisplayName("should unescape \\u001F")
        void unescapeUpperControlChar() {
            assertEquals("\037", StringEscaper.unescape("\"\\u001f\""));
        }

        @Test
        @DisplayName("should throw on surrogate pair in \\u escapes")
        void unescapeSurrogatePair() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> StringEscaper.unescape("\"\\uD800\\uDC00\""));
            assertTrue(ex.getMessage().contains("surrogate"));
        }

        @Test
        @DisplayName("should throw on truncated \\u escape")
        void truncatedUnicodeEscape() {
            assertThrows(IllegalArgumentException.class,
                () -> StringEscaper.unescape("\"\\u00b\""));
        }

        @Test
        @DisplayName("should throw on invalid hex in \\u escape")
        void invalidUnicodeHex() {
            assertThrows(IllegalArgumentException.class,
                () -> StringEscaper.unescape("\"\\u00XX\""));
        }

        @Test
        @DisplayName("should throw on lone low surrogate in \\u escape")
        void loneLowSurrogate() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> StringEscaper.unescape("\"\\uDC00\""));
            assertTrue(ex.getMessage().contains("surrogate"));
        }

        @Test
        @DisplayName("should throw on lone high surrogate in \\u escape")
        void loneHighSurrogate() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> StringEscaper.unescape("\"\\uD800\""));
            assertTrue(ex.getMessage().contains("surrogate"));
        }

    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<StringEscaper> constructor = StringEscaper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When
        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        // Then
        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    void testingValidateString_WithNotQuotedString() {
        // covers startsWith(\") = false branch on lines 68 and 73
        StringEscaper.validateString("plain text without quotes");
    }

    @Test
    void testingValidateString_WithNull() {
        // Given
        final String input = null;
        // When
        StringEscaper.validateString(input);
        // Then

    }

    @Test
    void testingValidateString_WithEmptyString() {
        // Given
        final String input = "";
        // When
        StringEscaper.validateString(input);
        // Then

    }

    @Test
    void testingValidateString_WithWildStringToThrowsException() {
        // Given
        final String input = "\"te\\st\"";
        // When      // Then
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> {
                StringEscaper.validateString(input);
            });

        assertEquals("Invalid escape sequence: \\s", thrown.getMessage());
    }

    @Test
    void testingValidateString_WithWildStringOnlyAtTheStartToThrowsException() {
        // Given
        final String input = "\"te\\st";
        // When      // Then
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> {
                StringEscaper.validateString(input);
            });

        assertEquals("Unterminated string", thrown.getMessage());
    }

    @Test
    void testingValidateString_WithWildStringOnlyAtTheStartAndEndToThrowsException() {
        // Given
        final String input = "\"abc\\\"";
        // When      // Then
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> {
                StringEscaper.validateString(input);
            });

        assertEquals("Invalid escape sequence: trailing backslash", thrown.getMessage());
    }
}
