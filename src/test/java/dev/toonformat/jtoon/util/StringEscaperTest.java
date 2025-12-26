package dev.toonformat.jtoon.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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
        void testBasicEscaping(String description, String input, String expected) {
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
        void testCombinedEscaping(String description, String input, String expected) {
            assertEquals(expected, StringEscaper.escape(input));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should return empty string for empty input")
        void testEmptyString() {
            assertEquals("", StringEscaper.escape(""));
        }

        @ParameterizedTest
        @DisplayName("should not modify strings without special characters")
        @ValueSource(strings = {
            "hello world",
            "Hello World 123 @#$%^&*()_+-=[]{}|;:',.<>?/",
            "Hello 世界 🌍"
        })
        void testStringsWithoutSpecialCharacters(String input) {
            assertEquals(input, StringEscaper.escape(input));
        }

        @Test
        @DisplayName("should handle consecutive backslashes")
        void testConsecutiveBackslashes() {
            String input = "\\\\\\";
            String expected = "\\\\\\\\\\\\";
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
        void testRealWorldScenarios(String scenario, String input, String expected) {
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
        void testBasicUnescaping(String description, String input, String expected) {
            assertEquals(expected, StringEscaper.unescape(input));
        }
    }

    @Nested
    @DisplayName("Quote Removal")
    class QuoteRemoval {

        @Test
        @DisplayName("should remove surrounding quotes")
        void testQuoteRemoval() {
            assertEquals("hello", StringEscaper.unescape("\"hello\""));
        }

        @Test
        @DisplayName("should handle quotes with escaped content")
        void testQuotedEscapedContent() {
            assertEquals("hello\nworld", StringEscaper.unescape("\"hello\\nworld\""));
        }

        @Test
        @DisplayName("should not remove quotes if not surrounding")
        void testNonSurroundingQuotes() {
            assertEquals("hello\"world", StringEscaper.unescape("hello\"world"));
        }

        @Test
        @DisplayName("should handle empty quoted string")
        void testEmptyQuotedString() {
            assertEquals("", StringEscaper.unescape("\"\""));
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
        void testRoundTrip(String original) {
            String escaped = StringEscaper.escape(original);
            String unescaped = StringEscaper.unescape("\"" + escaped + "\"");
            assertEquals(original, unescaped);
        }
    }

    @Nested
    @DisplayName("Unescape Edge Cases")
    class UnescapeEdgeCases {

        @Test
        @DisplayName("should handle null input")
        void testNullInput() {
            assertNull(StringEscaper.unescape(null));
        }

        @Test
        @DisplayName("should handle empty string")
        void testEmptyString() {
            assertEquals("", StringEscaper.unescape(""));
        }

        @Test
        @DisplayName("should handle single character")
        void testSingleCharacter() {
            assertEquals("a", StringEscaper.unescape("a"));
        }

        @Test
        @DisplayName("should handle strings without escape sequences")
        void testNoEscapeSequences() {
            assertEquals("hello world", StringEscaper.unescape("hello world"));
        }

        @Test
        @DisplayName("should handle unknown escape sequences as literals")
        void testUnknownEscapeSequences() {
            assertEquals("ax", StringEscaper.unescape("\\ax"));
        }

        @Test
        void unquotesValueWhenStartsAndEndsWithQuote() {
            assertEquals("abc", StringEscaper.unescape("\"abc\""));
        }

        @Test
        void unescapesBackslashSequences() {
            assertEquals("a\"b", StringEscaper.unescape("a\\\"b"));
        }

        @Test
        void unescapesMultipleCharacters() {
            assertEquals("a\nb\tc", StringEscaper.unescape("a\\nb\\tc"));
        }

        @Test
        void handlesTrailingBackslashCorrectly() {
            // trailing \ will set escaped=true but there is no next char → nothing appended
            assertEquals("abc", StringEscaper.unescape("abc\\"));
        }

        @Test
        void handlesDoubleBackslashCorrectly() {
            assertEquals("a\\b", StringEscaper.unescape("a\\\\b"));
        }
    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<StringEscaper> constructor = StringEscaper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    void testingValidateString_WithNull() {
        // Given
        String input = null;
        // When
        StringEscaper.validateString(input);
        // Then

    }

    @Test
    void testingValidateString_WithEmptyString() {
        // Given
        String input = "";
        // When
        StringEscaper.validateString(input);
        // Then

    }

    @Test
    void testingValidateString_WithWildStringToThrowsException() {
        // Given
        String input = "\"te\\st\"";
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
        String input = "\"te\\st";
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
        String input = "\"abc\\\"";
        // When      // Then
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> {
                StringEscaper.validateString(input);
            });

        assertEquals("Invalid escape sequence: trailing backslash", thrown.getMessage());
    }
}
