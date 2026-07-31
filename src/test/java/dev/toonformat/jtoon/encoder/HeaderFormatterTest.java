package dev.toonformat.jtoon.encoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Stream;
import dev.toonformat.jtoon.Delimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for HeaderFormatter utility class.
 * Tests header formatting for arrays and tabular structures in TOON format.
 */
@Tag("unit")
public class HeaderFormatterTest {

    private static List<TabularField> leafFields(final String... names) {
        return java.util.Arrays.stream(names).map(TabularField::leaf).toList();
    }

    @Nested
    @DisplayName("Simple Array Headers")
    class SimpleArrayHeaders {

        @Test
        @DisplayName("should format simple array header without key")
        void testSimpleArrayWithoutKey() {
            // Given
            final String result = HeaderFormatter.format(3, null, null, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("[3]:", result);
        }

        @Test
        @DisplayName("should format simple array header with key")
        void testSimpleArrayWithKey() {
            // Given
            final String result = HeaderFormatter.format(5, "items", null, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("items[5]:", result);
        }

        @Test
        @DisplayName("should format empty array")
        void testEmptyArray() {
            // Given
            final String result = HeaderFormatter.format(0, "items", null, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("items[0]:", result);
        }

        @Test
        @DisplayName("should format array with length marker")
        void testArrayWithLengthMarker() {
            // Given
            final String result = HeaderFormatter.format(3, "items", null, Delimiter.COMMA.toString(), true);

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
            final List<TabularField> fields = leafFields("id", "name", "age");

            // When
            final String result = HeaderFormatter.format(2, "users", fields, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("users[2]{id,name,age}:", result);
        }

        @Test
        @DisplayName("should format tabular header with single field")
        void testSingleField() {
            // Given
            final List<TabularField> fields = leafFields("value");

            // When
            final String result = HeaderFormatter.format(5, "data", fields, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("data[5]{value}:", result);
        }

        @Test
        @DisplayName("should format tabular header without key")
        void testTabularWithoutKey() {
            // Given
            final List<TabularField> fields = leafFields("x", "y");

            // When
            final String result = HeaderFormatter.format(10, null, fields, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("[10]{x,y}:", result);
        }

        @Test
        @DisplayName("should format empty tabular header (no fields)")
        void testEmptyFields() {
            // Given
            final List<TabularField> fields = leafFields();

            // When
            final String result = HeaderFormatter.format(3, "items", fields, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("items[3]:", result);
        }

        @Test
        @DisplayName("should format tabular header with length marker")
        void testTabularWithLengthMarker() {
            // Given
            final List<TabularField> fields = leafFields("id", "name");

            // When
            final String result = HeaderFormatter.format(2, "users", fields, Delimiter.COMMA.toString(), true);

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
        void testDelimiterFormatting(final String delimiterName, final String delimiter, final String expected) {
            // Given
            final List<TabularField> fields = leafFields("a", "b", "c");

            // When
            final String result = HeaderFormatter.format(3, "data", fields, delimiter, false);

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
            final String result = HeaderFormatter.format(5, "items", null, Delimiter.PIPE.toString(), false);

            // Then
            assertEquals("items[5|]:", result);
        }

        @Test
        @DisplayName("should format array with tab delimiter")
        void testArrayWithTabDelimiter() {
            // Given
            final String result = HeaderFormatter.format(5, "items", null, Delimiter.TAB.toString(), false);

            // Then
            assertEquals("items[5\t]:", result);
        }

        @Test
        @DisplayName("should format with pipe delimiter and length marker")
        void testPipeWithLengthMarker() {
            // Given
            final List<TabularField> fields = leafFields("x", "y");

            // When
            final String result = HeaderFormatter.format(2, "points", fields, Delimiter.PIPE.toString(), true);

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
            final String result = HeaderFormatter.format(3, "my items", null, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("\"my items\"[3]:", result);
        }

        @Test
        @DisplayName("should quote numeric key")
        void testNumericKey() {
            // Given
            final String result = HeaderFormatter.format(2, "123", null, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("\"123\"[2]:", result);
        }

        @Test
        @DisplayName("should not quote simple alphanumeric key")
        void testSimpleKey() {
            // Given
            final String result = HeaderFormatter.format(3, "items", null, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("items[3]:", result);
        }

        @Test
        @DisplayName("should quote field names with special characters")
        void testFieldQuoting() {
            // Given
            final List<TabularField> fields = leafFields("first name", "last name");

            // When
            final String result = HeaderFormatter.format(2, "users", fields, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("users[2]{\"first name\",\"last name\"}:", result);
        }

        @Test
        @DisplayName("should handle mix of quoted and unquoted field names")
        void testMixedFieldQuoting() {
            // Given
            final List<TabularField> fields = leafFields("id", "full name", "age");

            // When
            final String result = HeaderFormatter.format(2, "users", fields, Delimiter.COMMA.toString(), false);

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
            final HeaderFormatter.HeaderConfig config = new HeaderFormatter.HeaderConfig(
                3, "items", leafFields("id", "name"), Delimiter.COMMA.toString(), false);

            // When
            final String result = HeaderFormatter.format(config);

            // Then
            assertEquals("items[3]{id,name}:", result);
        }

        @Test
        @DisplayName("should format using record with null key")
        void testRecordWithNullKey() {
            // Given
            final HeaderFormatter.HeaderConfig config = new HeaderFormatter.HeaderConfig(
                5, null, null, Delimiter.COMMA.toString(), false);

            // When
            final String result = HeaderFormatter.format(config);

            // Then
            assertEquals("[5]:", result);
        }

        @Test
        @DisplayName("should format using record with pipe delimiter")
        void testRecordWithPipeDelimiter() {
            // Given
            final HeaderFormatter.HeaderConfig config = new HeaderFormatter.HeaderConfig(
                2, "data", leafFields("x", "y"), Delimiter.PIPE.toString(), true);
            // When
            final String result = HeaderFormatter.format(config);

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
            final String result = HeaderFormatter.format(999999, "data", null, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("data[999999]:", result);
        }

        @Test
        @DisplayName("should handle zero length with fields")
        void testZeroLengthWithFields() {
            // Given
            final List<TabularField> fields = leafFields("id", "name");

            // When
            final String result = HeaderFormatter.format(0, "empty", fields, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("empty[0]{id,name}:", result);
        }

        @Test
        @DisplayName("should handle many fields")
        void testManyFields() {
            // Given
            final List<TabularField> fields = leafFields("f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10");

            // When
            final String result = HeaderFormatter.format(1, "data", fields, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("data[1]{f1,f2,f3,f4,f5,f6,f7,f8,f9,f10}:", result);
        }

        @Test
        @DisplayName("should handle null fields list (treated as no fields)")
        void testNullFields() {
            // Given
            final String result = HeaderFormatter.format(3, "items", null, Delimiter.COMMA.toString(), false);

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
            final List<TabularField> fields = leafFields("id", "name", "stars", "forks");

            // When
            final String result = HeaderFormatter.format(100, "repositories", fields,
                Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("repositories[100]{id,name,stars,forks}:", result);
        }

        @Test
        @DisplayName("should format analytics metrics header")
        void testAnalyticsMetrics() {
            // Given
            final List<TabularField> fields = leafFields("date", "views", "clicks", "conversions", "revenue");

            // When
            final String result = HeaderFormatter.format(180, "metrics", fields, ",", false);

            // Then
            assertEquals("metrics[180]{date,views,clicks,conversions,revenue}:", result);
        }

        @Test
        @DisplayName("should format employee records with tab delimiter")
        void testEmployeeRecords() {
            // Given
            final List<TabularField> fields = leafFields("id", "name", "department", "salary");

            // When
            final String result = HeaderFormatter.format(50, "employees", fields, Delimiter.TAB.toString(), false);

            // Then
            assertEquals("employees[50\t]{id\tname\tdepartment\tsalary}:", result);
        }

        @Test
        @DisplayName("should format nested array in list item")
        void testNestedArray() {
            // Given
            final List<TabularField> fields = leafFields("sku", "qty", "price");

            // When
            final String result = HeaderFormatter.format(3, "items", fields, Delimiter.COMMA.toString(), false);

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
