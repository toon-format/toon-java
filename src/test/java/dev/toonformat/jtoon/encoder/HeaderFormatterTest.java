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
            String result = HeaderFormatter.format(3, null, null, ",", false);
            assertEquals("[3]:", result);
        }

        @Test
        @DisplayName("should format simple array header with key")
        void testSimpleArrayWithKey() {
            String result = HeaderFormatter.format(5, "items", null, ",", false);
            assertEquals("items[5]:", result);
        }

        @Test
        @DisplayName("should format empty array")
        void testEmptyArray() {
            String result = HeaderFormatter.format(0, "items", null, ",", false);
            assertEquals("items[0]:", result);
        }

        @Test
        @DisplayName("should format array with length marker")
        void testArrayWithLengthMarker() {
            String result = HeaderFormatter.format(3, "items", null, ",", true);
            assertEquals("items[#3]:", result);
        }
    }

    @Nested
    @DisplayName("Tabular Headers")
    class TabularHeaders {

        @Test
        @DisplayName("should format tabular header with fields")
        void testTabularHeader() {
            List<String> fields = List.of("id", "name", "age");
            String result = HeaderFormatter.format(2, "users", fields, ",", false);
            assertEquals("users[2]{id,name,age}:", result);
        }

        @Test
        @DisplayName("should format tabular header with single field")
        void testSingleField() {
            List<String> fields = List.of("value");
            String result = HeaderFormatter.format(5, "data", fields, ",", false);
            assertEquals("data[5]{value}:", result);
        }

        @Test
        @DisplayName("should format tabular header without key")
        void testTabularWithoutKey() {
            List<String> fields = List.of("x", "y");
            String result = HeaderFormatter.format(10, null, fields, ",", false);
            assertEquals("[10]{x,y}:", result);
        }

        @Test
        @DisplayName("should format empty tabular header (no fields)")
        void testEmptyFields() {
            List<String> fields = List.of();
            String result = HeaderFormatter.format(3, "items", fields, ",", false);
            assertEquals("items[3]:", result);
        }

        @Test
        @DisplayName("should format tabular header with length marker")
        void testTabularWithLengthMarker() {
            List<String> fields = List.of("id", "name");
            String result = HeaderFormatter.format(2, "users", fields, ",", true);
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
            List<String> fields = List.of("a", "b", "c");
            String result = HeaderFormatter.format(3, "data", fields, delimiter, false);
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
            String result = HeaderFormatter.format(5, "items", null, "|", false);
            assertEquals("items[5|]:", result);
        }

        @Test
        @DisplayName("should format array with tab delimiter")
        void testArrayWithTabDelimiter() {
            String result = HeaderFormatter.format(5, "items", null, "\t", false);
            assertEquals("items[5\t]:", result);
        }

        @Test
        @DisplayName("should format with pipe delimiter and length marker")
        void testPipeWithLengthMarker() {
            List<String> fields = List.of("x", "y");
            String result = HeaderFormatter.format(2, "points", fields, "|", true);
            assertEquals("points[#2|]{x|y}:", result);
        }
    }

    @Nested
    @DisplayName("Key Quoting")
    class KeyQuoting {

        @Test
        @DisplayName("should quote key with spaces")
        void testKeyWithSpaces() {
            String result = HeaderFormatter.format(3, "my items", null, ",", false);
            assertEquals("\"my items\"[3]:", result);
        }

        @Test
        @DisplayName("should quote numeric key")
        void testNumericKey() {
            String result = HeaderFormatter.format(2, "123", null, ",", false);
            assertEquals("\"123\"[2]:", result);
        }

        @Test
        @DisplayName("should not quote simple alphanumeric key")
        void testSimpleKey() {
            String result = HeaderFormatter.format(3, "items", null, ",", false);
            assertEquals("items[3]:", result);
        }

        @Test
        @DisplayName("should quote field names with special characters")
        void testFieldQuoting() {
            List<String> fields = List.of("first name", "last name");
            String result = HeaderFormatter.format(2, "users", fields, ",", false);
            assertEquals("users[2]{\"first name\",\"last name\"}:", result);
        }

        @Test
        @DisplayName("should handle mix of quoted and unquoted field names")
        void testMixedFieldQuoting() {
            List<String> fields = List.of("id", "full name", "age");
            String result = HeaderFormatter.format(2, "users", fields, ",", false);
            assertEquals("users[2]{id,\"full name\",age}:", result);
        }
    }

    @Nested
    @DisplayName("Record-Based Format Method")
    class RecordBasedFormat {

        @Test
        @DisplayName("should format using HeaderConfig record")
        void testRecordFormat() {
            HeaderFormatter.HeaderConfig config = new HeaderFormatter.HeaderConfig(
                    3, "items", List.of("id", "name"), ",", false);
            String result = HeaderFormatter.format(config);
            assertEquals("items[3]{id,name}:", result);
        }

        @Test
        @DisplayName("should format using record with null key")
        void testRecordWithNullKey() {
            HeaderFormatter.HeaderConfig config = new HeaderFormatter.HeaderConfig(
                    5, null, null, ",", false);
            String result = HeaderFormatter.format(config);
            assertEquals("[5]:", result);
        }

        @Test
        @DisplayName("should format using record with pipe delimiter")
        void testRecordWithPipeDelimiter() {
            HeaderFormatter.HeaderConfig config = new HeaderFormatter.HeaderConfig(
                    2, "data", List.of("x", "y"), "|", true);
            String result = HeaderFormatter.format(config);
            assertEquals("data[#2|]{x|y}:", result);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle large array length")
        void testLargeLength() {
            String result = HeaderFormatter.format(999999, "data", null, ",", false);
            assertEquals("data[999999]:", result);
        }

        @Test
        @DisplayName("should handle zero length with fields")
        void testZeroLengthWithFields() {
            List<String> fields = List.of("id", "name");
            String result = HeaderFormatter.format(0, "empty", fields, ",", false);
            assertEquals("empty[0]{id,name}:", result);
        }

        @Test
        @DisplayName("should handle many fields")
        void testManyFields() {
            List<String> fields = List.of("f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10");
            String result = HeaderFormatter.format(1, "data", fields, ",", false);
            assertEquals("data[1]{f1,f2,f3,f4,f5,f6,f7,f8,f9,f10}:", result);
        }

        @Test
        @DisplayName("should handle null fields list (treated as no fields)")
        void testNullFields() {
            String result = HeaderFormatter.format(3, "items", null, ",", false);
            assertEquals("items[3]:", result);
        }
    }

    @Nested
    @DisplayName("Real-World Examples")
    class RealWorldExamples {

        @Test
        @DisplayName("should format GitHub repositories header")
        void testGitHubRepos() {
            List<String> fields = List.of("id", "name", "stars", "forks");
            String result = HeaderFormatter.format(100, "repositories", fields, ",", false);
            assertEquals("repositories[100]{id,name,stars,forks}:", result);
        }

        @Test
        @DisplayName("should format analytics metrics header")
        void testAnalyticsMetrics() {
            List<String> fields = List.of("date", "views", "clicks", "conversions", "revenue");
            String result = HeaderFormatter.format(180, "metrics", fields, ",", false);
            assertEquals("metrics[180]{date,views,clicks,conversions,revenue}:", result);
        }

        @Test
        @DisplayName("should format employee records with tab delimiter")
        void testEmployeeRecords() {
            List<String> fields = List.of("id", "name", "department", "salary");
            String result = HeaderFormatter.format(50, "employees", fields, "\t", false);
            assertEquals("employees[50\t]{id\tname\tdepartment\tsalary}:", result);
        }

        @Test
        @DisplayName("should format nested array in list item")
        void testNestedArray() {
            List<String> fields = List.of("sku", "qty", "price");
            String result = HeaderFormatter.format(3, "items", fields, ",", false);
            assertEquals("items[3]{sku,qty,price}:", result);
        }
    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<HeaderFormatter> constructor = HeaderFormatter.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }
}
