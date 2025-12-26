package dev.toonformat.jtoon.encoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HeaderFormatter utility class.
 * Tests header formatting for arrays and tabular structures in TOON format.
 */
@Tag("unit")
public class HeaderFormatterTest {

    @Nested
    @DisplayName("Simple Array Headers")
    class SimpleArrayHeaders {

        @Test
        @DisplayName("should format simple array header without key")
        void testSimpleArrayWithoutKey() {
            // Given
            String result = HeaderFormatter.format(3, null, null, ",", false);

            // Then
            assertEquals("[3]:", result);
        }

        @Test
        @DisplayName("should format simple array header with key")
        void testSimpleArrayWithKey() {
            // Given
            String result = HeaderFormatter.format(5, "items", null, ",", false);

            // Then
            assertEquals("items[5]:", result);
        }

        @Test
        @DisplayName("should format empty array")
        void testEmptyArray() {
            // Given
            String result = HeaderFormatter.format(0, "items", null, ",", false);

            // Then
            assertEquals("items[0]:", result);
        }

        @Test
        @DisplayName("should format array with length marker")
        void testArrayWithLengthMarker() {
            // Given
            String result = HeaderFormatter.format(3, "items", null, ",", true);

            // Then
            assertEquals("items[#3]:", result);
        }
    }

    @Nested
    @DisplayName("Tabular Headers")
    class TabularHeaders {

        @Test
        @DisplayName("should format tabular header with fields")
        void testTabularHeader() {
            // Given
            List<String> fields = List.of("id", "name", "age");

            // When
            String result = HeaderFormatter.format(2, "users", fields, ",", false);

            // Then
            assertEquals("users[2]{id,name,age}:", result);
        }

        @Test
        @DisplayName("should format tabular header with single field")
        void testSingleField() {
            // Given
            List<String> fields = List.of("value");

            // When
            String result = HeaderFormatter.format(5, "data", fields, ",", false);

            // Then
            assertEquals("data[5]{value}:", result);
        }

        @Test
        @DisplayName("should format tabular header without key")
        void testTabularWithoutKey() {
            // Given
            List<String> fields = List.of("x", "y");

            // When
            String result = HeaderFormatter.format(10, null, fields, ",", false);

            // Then
            assertEquals("[10]{x,y}:", result);
        }

        @Test
        @DisplayName("should format empty tabular header (no fields)")
        void testEmptyFields() {
            // Given
            List<String> fields = List.of();

            // When
            String result = HeaderFormatter.format(3, "items", fields, ",", false);

            // Then
            assertEquals("items[3]:", result);
        }

        @Test
        @DisplayName("should format tabular header with length marker")
        void testTabularWithLengthMarker() {
            // Given
            List<String> fields = List.of("id", "name");

            // When
            String result = HeaderFormatter.format(2, "users", fields, ",", true);

            // Then
            assertEquals("users[#2]{id,name}:", result);
        }
    }

    @Nested
    @DisplayName("Delimiter Variations")
    class DelimiterVariations {

        @ParameterizedTest(name = "should format with {0} delimiter")
        @MethodSource("delimiterTestData")
        @DisplayName("should format with different delimiters")
        void testDelimiterFormatting(String delimiterName, String delimiter, String expected) {
            // Given
            List<String> fields = List.of("a", "b", "c");

            // When
            String result = HeaderFormatter.format(3, "data", fields, delimiter, false);

            // Then
            assertEquals(expected, result);
        }

        static Stream<Arguments> delimiterTestData() {
            return Stream.of(
                Arguments.of("comma (implicit)", ",", "data[3]{a,b,c}:"),
                Arguments.of("pipe (explicit)", "|", "data[3|]{a|b|c}:"),
                Arguments.of("tab (explicit)", "\t", "data[3\t]{a\tb\tc}:"));
        }

        @Test
        @DisplayName("should format array with pipe delimiter")
        void testArrayWithPipeDelimiter() {
            // Given
            String result = HeaderFormatter.format(5, "items", null, "|", false);

            // Then
            assertEquals("items[5|]:", result);
        }

        @Test
        @DisplayName("should format array with tab delimiter")
        void testArrayWithTabDelimiter() {
            // Given
            String result = HeaderFormatter.format(5, "items", null, "\t", false);

            // Then
            assertEquals("items[5\t]:", result);
        }

        @Test
        @DisplayName("should format with pipe delimiter and length marker")
        void testPipeWithLengthMarker() {
            // Given
            List<String> fields = List.of("x", "y");

            // When
            String result = HeaderFormatter.format(2, "points", fields, "|", true);

            // Then
            assertEquals("points[#2|]{x|y}:", result);
        }
    }

    @Nested
    @DisplayName("Key Quoting")
    class KeyQuoting {

        @Test
        @DisplayName("should quote key with spaces")
        void testKeyWithSpaces() {
            // Given
            String result = HeaderFormatter.format(3, "my items", null, ",", false);

            // Then
            assertEquals("\"my items\"[3]:", result);
        }

        @Test
        @DisplayName("should quote numeric key")
        void testNumericKey() {
            // Given
            String result = HeaderFormatter.format(2, "123", null, ",", false);

            // Then
            assertEquals("\"123\"[2]:", result);
        }

        @Test
        @DisplayName("should not quote simple alphanumeric key")
        void testSimpleKey() {
            // Given
            String result = HeaderFormatter.format(3, "items", null, ",", false);

            // Then
            assertEquals("items[3]:", result);
        }

        @Test
        @DisplayName("should quote field names with special characters")
        void testFieldQuoting() {
            // Given
            List<String> fields = List.of("first name", "last name");

            // When
            String result = HeaderFormatter.format(2, "users", fields, ",", false);

            // Then
            assertEquals("users[2]{\"first name\",\"last name\"}:", result);
        }

        @Test
        @DisplayName("should handle mix of quoted and unquoted field names")
        void testMixedFieldQuoting() {
            // Given
            List<String> fields = List.of("id", "full name", "age");

            // When
            String result = HeaderFormatter.format(2, "users", fields, ",", false);

            // Then
            assertEquals("users[2]{id,\"full name\",age}:", result);
        }
    }

    @Nested
    @DisplayName("Record-Based Format Method")
    class RecordBasedFormat {

        @Test
        @DisplayName("should format using HeaderConfig record")
        void testRecordFormat() {
            // Given
            HeaderFormatter.HeaderConfig config = new HeaderFormatter.HeaderConfig(
                3, "items", List.of("id", "name"), ",", false);

            // When
            String result = HeaderFormatter.format(config);

            // Then
            assertEquals("items[3]{id,name}:", result);
        }

        @Test
        @DisplayName("should format using record with null key")
        void testRecordWithNullKey() {
            // Given
            HeaderFormatter.HeaderConfig config = new HeaderFormatter.HeaderConfig(
                5, null, null, ",", false);

            // When
            String result = HeaderFormatter.format(config);

            // Then
            assertEquals("[5]:", result);
        }

        @Test
        @DisplayName("should format using record with pipe delimiter")
        void testRecordWithPipeDelimiter() {
            // Given
            HeaderFormatter.HeaderConfig config = new HeaderFormatter.HeaderConfig(
                2, "data", List.of("x", "y"), "|", true);
            // When
            String result = HeaderFormatter.format(config);

            // Then
            assertEquals("data[#2|]{x|y}:", result);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle large array length")
        void testLargeLength() {
            // Given
            String result = HeaderFormatter.format(999999, "data", null, ",", false);

            // Then
            assertEquals("data[999999]:", result);
        }

        @Test
        @DisplayName("should handle zero length with fields")
        void testZeroLengthWithFields() {
            // Given
            List<String> fields = List.of("id", "name");

            // When
            String result = HeaderFormatter.format(0, "empty", fields, ",", false);

            // Then
            assertEquals("empty[0]{id,name}:", result);
        }

        @Test
        @DisplayName("should handle many fields")
        void testManyFields() {
            // Given
            List<String> fields = List.of("f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10");

            // When
            String result = HeaderFormatter.format(1, "data", fields, ",", false);

            // Then
            assertEquals("data[1]{f1,f2,f3,f4,f5,f6,f7,f8,f9,f10}:", result);
        }

        @Test
        @DisplayName("should handle null fields list (treated as no fields)")
        void testNullFields() {
            // Given
            String result = HeaderFormatter.format(3, "items", null, ",", false);

            // Then
            assertEquals("items[3]:", result);
        }
    }

    @Nested
    @DisplayName("Real-World Examples")
    class RealWorldExamples {

        @Test
        @DisplayName("should format GitHub repositories header")
        void testGitHubRepos() {
            // Given
            List<String> fields = List.of("id", "name", "stars", "forks");

            // When
            String result = HeaderFormatter.format(100, "repositories", fields, ",", false);

            // Then
            assertEquals("repositories[100]{id,name,stars,forks}:", result);
        }

        @Test
        @DisplayName("should format analytics metrics header")
        void testAnalyticsMetrics() {
            // Given
            List<String> fields = List.of("date", "views", "clicks", "conversions", "revenue");

            // When
            String result = HeaderFormatter.format(180, "metrics", fields, ",", false);

            // Then
            assertEquals("metrics[180]{date,views,clicks,conversions,revenue}:", result);
        }

        @Test
        @DisplayName("should format employee records with tab delimiter")
        void testEmployeeRecords() {
            // Given
            List<String> fields = List.of("id", "name", "department", "salary");

            // When
            String result = HeaderFormatter.format(50, "employees", fields, "\t", false);

            // Then
            assertEquals("employees[50\t]{id\tname\tdepartment\tsalary}:", result);
        }

        @Test
        @DisplayName("should format nested array in list item")
        void testNestedArray() {
            // Given
            List<String> fields = List.of("sku", "qty", "price");

            // When
            String result = HeaderFormatter.format(3, "items", fields, ",", false);

            // Then
            assertEquals("items[3]{sku,qty,price}:", result);
        }
    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<HeaderFormatter> constructor = HeaderFormatter.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When
        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        // Then
        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }
}
