package dev.toonformat.jtoon.util;

import dev.toonformat.jtoon.Delimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StringValidator utility class.
 * Tests validation logic for safe unquoted strings and keys in TOON format.
 */
@Tag("unit")
class StringValidatorTest {

    @Nested
    @DisplayName("isSafeUnquoted - Basic Cases")
    class IsSafeUnquotedBasic {

        @Test
        @DisplayName("should return false for null")
        void testNullValue() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted(null, Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for empty string")
        void testEmptyString() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return true for simple alphanumeric string")
        void testSimpleString() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("hello123", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return true for string with spaces")
        void testStringWithInnerSpaces() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("hello world", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for a number")
        void testNumber() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("123456", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for a Scientific Notation number")
        void testScientificNumber() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("-2.5E-8", Delimiter.COMMA.toString()));
            assertFalse(StringValidator.isSafeUnquoted("1e10", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for a octal number")
        void testOctalNumber() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("07", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for a number with a leading zero")
        void testLeadingZeroNumber() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("0.07", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for a negative number with a leading zero")
        void testLeadingNegativeZeroNumber() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("-0.07", Delimiter.COMMA.toString()));
        }
    }

    @Nested
    @DisplayName("isSafeUnquoted - Whitespace Padding")
    class WhitespacePadding {

        @Test
        @DisplayName("should return false for leading space")
        void testLeadingSpace() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted(" hello", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for trailing space")
        void testTrailingSpace() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("hello ", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for both leading and trailing spaces")
        void testBothSpaces() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted(" hello ", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for only spaces")
        void testOnlySpaces() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("   ", Delimiter.COMMA.toString()));
        }
    }

    @Nested
    @DisplayName("isSafeUnquoted - Keywords")
    class Keywords {

        @Test
        @DisplayName("should return false for 'true'")
        void testTrueKeyword() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("true", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for 'false'")
        void testFalseKeyword() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("false", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for 'null'")
        void testNullKeyword() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("null", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return true for 'True' (case sensitive)")
        void testTrueCaseSensitive() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("True", Delimiter.COMMA.toString()));
        }
    }

    @Nested
    @DisplayName("isSafeUnquoted - Numbers")
    class Numbers {

        @Test
        @DisplayName("should return false for integer")
        void testInteger() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("123", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for negative integer")
        void testNegativeInteger() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("-456", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for decimal")
        void testDecimal() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("3.14", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for scientific notation")
        void testScientificNotation() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("1.5e10", Delimiter.COMMA.toString()));
            assertFalse(StringValidator.isSafeUnquoted("1.5E-10", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for octal-like numbers")
        void testOctalNumber() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("0123", Delimiter.COMMA.toString()));
            assertFalse(StringValidator.isSafeUnquoted("0777", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return true for text starting with number")
        void testTextStartingWithNumber() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("123abc", Delimiter.COMMA.toString()));
        }
    }

    @Nested
    @DisplayName("isSafeUnquoted - Special Characters")
    class SpecialCharacters {

        @Test
        @DisplayName("should return false for string with colon")
        void testColon() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("key:value", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for string with double quote")
        void testDoubleQuote() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("say \"hi\"", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for string with backslash")
        void testBackslash() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("path\\file", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for string with brackets")
        void testBrackets() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("array[0]", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for string with braces")
        void testBraces() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("obj{key}", Delimiter.COMMA.toString()));
        }
    }

    @Nested
    @DisplayName("isSafeUnquoted - Control Characters")
    class ControlCharacters {

        @Test
        @DisplayName("should return false for newline")
        void testNewline() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("line1\nline2", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for carriage return")
        void testCarriageReturn() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("line1\rline2", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for tab")
        void testTab() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("col1\tcol2", Delimiter.COMMA.toString()));
        }
    }

    @Nested
    @DisplayName("isSafeUnquoted - Delimiter Aware")
    class DelimiterAware {

        @Test
        @DisplayName("should return false for comma with comma delimiter")
        void testCommaDelimiter() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("a,b", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return true for comma with pipe delimiter")
        void testCommaWithPipeDelimiter() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("a,b", Delimiter.PIPE.toString()));
        }

        @Test
        @DisplayName("should return false for pipe with pipe delimiter")
        void testPipeDelimiter() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("a|b", Delimiter.PIPE.toString()));
        }

        @Test
        @DisplayName("should return true for pipe with comma delimiter")
        void testPipeWithCommaDelimiter() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("a|b", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for tab with tab delimiter")
        void testTabDelimiter() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("a\tb", Delimiter.TAB.toString()));
        }

        @Test
        @DisplayName("should return true for tab with comma delimiter")
        void testTabWithCommaDelimiter() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("a\tb", Delimiter.COMMA.toString())); // Still false due to control char
        }
    }

    @Nested
    @DisplayName("isSafeUnquoted - List Marker")
    class ListMarker {

        @Test
        @DisplayName("should return false for string starting with list marker")
        void testListMarker() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("- item", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return true for string containing but not starting with dash-space")
        void testDashSpaceInMiddle() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("item - note", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return false for string starting with dash")
        void testDashWithoutSpace() {
            // Then
            assertFalse(StringValidator.isSafeUnquoted("-item", Delimiter.COMMA.toString()));
        }
    }

    @Nested
    @DisplayName("isSafeUnquoted - Safe Strings")
    class SafeStrings {

        @Test
        @DisplayName("should return true for alphanumeric with underscores")
        void testAlphanumericUnderscore() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("hello_world_123", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return true for string with hyphens")
        void testHyphens() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("hello-world", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return true for string with dots")
        void testDots() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("hello.world", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return true for Unicode characters")
        void testUnicode() {
            // Then
            assertTrue(StringValidator.isSafeUnquoted("Hello 世界", Delimiter.COMMA.toString()));
        }

        @Test
        @DisplayName("should return true for emoji")
        void testEmoji() {
            assertTrue(StringValidator.isSafeUnquoted("Hello 🌍", Delimiter.COMMA.toString()));
        }
    }

    @Nested
    @DisplayName("isValidUnquotedKey")
    class ValidUnquotedKey {

        @Test
        @DisplayName("should return true for simple alphanumeric key")
        void testSimpleKey() {
            // Then
            assertTrue(StringValidator.isValidUnquotedKey("key"));
        }

        @Test
        @DisplayName("should return true for key with underscores")
        void testKeyWithUnderscore() {
            // Then
            assertTrue(StringValidator.isValidUnquotedKey("my_key"));
            assertTrue(StringValidator.isValidUnquotedKey("_private"));
        }

        @Test
        @DisplayName("should return true for key with dots")
        void testKeyWithDots() {
            // Then
            assertTrue(StringValidator.isValidUnquotedKey("com.example.key"));
        }

        @Test
        @DisplayName("should return true for key with numbers")
        void testKeyWithNumbers() {
            // Then
            assertTrue(StringValidator.isValidUnquotedKey("key123"));
        }

        @Test
        @DisplayName("should return true for uppercase keys")
        void testUppercaseKey() {
            // Then
            assertTrue(StringValidator.isValidUnquotedKey("KEY"));
            assertTrue(StringValidator.isValidUnquotedKey("MyKey"));
        }

        @Test
        @DisplayName("should return false for key starting with number")
        void testKeyStartingWithNumber() {
            // Then
            assertFalse(StringValidator.isValidUnquotedKey("123key"));
        }

        @Test
        @DisplayName("should return false for key with spaces")
        void testKeyWithSpaces() {
            // Then
            assertFalse(StringValidator.isValidUnquotedKey("my key"));
        }

        @Test
        @DisplayName("should return false for key with hyphens")
        void testKeyWithHyphen() {
            assertFalse(StringValidator.isValidUnquotedKey("my-key"));
        }

        @Test
        @DisplayName("should return false for key with special characters")
        void testKeyWithSpecialChars() {
            // Then
            assertFalse(StringValidator.isValidUnquotedKey("key:value"));
            assertFalse(StringValidator.isValidUnquotedKey("key,value"));
            assertFalse(StringValidator.isValidUnquotedKey("key[0]"));
        }

        @Test
        @DisplayName("should return false for empty key")
        void testEmptyKey() {
            // Then
            assertFalse(StringValidator.isValidUnquotedKey(""));
        }
    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<StringValidator> constructor = StringValidator.class.getDeclaredConstructor();
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
    void returnsFalseForStringWithoutQuotesOrBackslash() {
        // Then
        assertFalse(StringValidator.containsQuotesOrBackslash("abc"));
    }

    @Test
    void detectsDoubleQuote() {
        // Then
        assertTrue(StringValidator.containsQuotesOrBackslash("a\"b"));
    }

    @Test
    void detectsBackslash() {
        // Then
        assertTrue(StringValidator.containsQuotesOrBackslash("a\\b"));
    }

    @Test
    void detectsBoth() {
        // Then
        assertTrue(StringValidator.containsQuotesOrBackslash("x\"y\\z"));
    }

    @Test
    void detectsQuoteAtStart() {
        // Then
        assertTrue(StringValidator.containsQuotesOrBackslash("\"abc"));
        assertTrue(StringValidator.containsQuotesOrBackslash("\\abc"));
        assertTrue(StringValidator.containsQuotesOrBackslash("x\"y\\z"));
    }

    @Test
    void detectsBackslashAtEnd() {
        // Then
        assertTrue(StringValidator.containsQuotesOrBackslash("abc\\"));
    }

    @Test
    void emptyStringReturnsFalse() {
        // Then
        assertFalse(StringValidator.containsQuotesOrBackslash(""));
    }
}

