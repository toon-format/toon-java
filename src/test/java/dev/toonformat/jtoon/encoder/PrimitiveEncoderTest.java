package dev.toonformat.jtoon.encoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PrimitiveEncoder utility class.
 * Tests encoding of primitive values, keys, and header formatting.
 */
@Tag("unit")
class PrimitiveEncoderTest {

    @Nested
    @DisplayName("encodePrimitive - Booleans")
    class EncodePrimitiveBoolean {

        @Test
        @DisplayName("should encode true")
        void testTrue() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(BooleanNode.TRUE, ",");

            // Then
            assertEquals("true", result);
        }

        @Test
        @DisplayName("should encode false")
        void testFalse() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(BooleanNode.FALSE, ",");

            // Then
            assertEquals("false", result);
        }
    }

    @Nested
    @DisplayName("encodePrimitive - Numbers")
    class EncodePrimitiveNumber {

        @Test
        @DisplayName("should encode integer")
        void testInteger() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(IntNode.valueOf(42), ",");

            // Then
            assertEquals("42", result);
        }

        @Test
        @DisplayName("should encode negative integer")
        void testNegativeInteger() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(IntNode.valueOf(-100), ",");

            // Then
            assertEquals("-100", result);
        }

        @Test
        @DisplayName("should encode zero")
        void testZero() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(IntNode.valueOf(0), ",");

            // Then
            assertEquals("0", result);
        }

        @Test
        @DisplayName("should encode long")
        void testLong() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(LongNode.valueOf(9999999999L), ",");

            // Then
            assertEquals("9999999999", result);
        }

        @Test
        @DisplayName("should encode double")
        void testDouble() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(DoubleNode.valueOf(3.14), ",");

            // Then
            assertEquals("3.14", result);
        }

        @Test
        @DisplayName("should encode float")
        void testFloat() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(FloatNode.valueOf(2.5f), ",");

            // Then
            assertEquals("2.5", result);
        }

        @Test
        @DisplayName("should encode decimal node")
        void testDecimal() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(DecimalNode.valueOf(new java.math.BigDecimal("123.456")), ",");

            // Then
            assertEquals("123.456", result);
        }
    }

    @Nested
    @DisplayName("encodePrimitive - Strings")
    class EncodePrimitiveString {

        @Test
        @DisplayName("should encode simple string unquoted")
        void testSimpleString() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf("hello"), ",");

            // Then
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("should quote string with comma when using comma delimiter")
        void testStringWithComma() {
            //give
            String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf("a,b"), ",");

            // Then
            assertEquals("\"a,b\"", result);
        }

        @Test
        @DisplayName("should not quote string with comma when using pipe delimiter")
        void testStringWithCommaUsingPipe() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf("a,b"), "|");

            // Then
            assertEquals("a,b", result);
        }

        @Test
        @DisplayName("should quote empty string")
        void testEmptyString() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf(""), ",");

            // Then
            assertEquals("\"\"", result);
        }

        @Test
        @DisplayName("should quote string that looks like boolean")
        void testBooleanLikeString() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf("true"), ",");

            // Then
            assertEquals("\"true\"", result);
        }

        @Test
        @DisplayName("should quote string that looks like null")
        void testNullLikeString() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf("null"), ",");

            // Then
            assertEquals("\"null\"", result);
        }

        @Test
        @DisplayName("should quote string that looks like number")
        void testNumberLikeString() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf("123"), ",");

            // Then
            assertEquals("\"123\"", result);
        }
    }

    @Nested
    @DisplayName("encodePrimitive - Null")
    class EncodePrimitiveNull {

        @Test
        @DisplayName("should encode null")
        void testNull() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(NullNode.getInstance(), ",");

            // Then
            assertEquals("null", result);
        }
    }

    @Nested
    @DisplayName("encodeStringLiteral")
    class EncodeStringLiteral {

        @Test
        @DisplayName("should encode simple string without quotes")
        void testSimpleString() {
            // Given
            String result = PrimitiveEncoder.encodeStringLiteral("hello world", ",");

            // Then
            assertEquals("hello world", result);
        }

        @Test
        @DisplayName("should quote and escape string with quotes")
        void testStringWithQuotes() {
            // Given
            String result = PrimitiveEncoder.encodeStringLiteral("say \"hi\"", ",");

            // Then
            assertEquals("\"say \\\"hi\\\"\"", result);
        }

        @Test
        @DisplayName("should quote string with leading space")
        void testLeadingSpace() {
            // Given
            String result = PrimitiveEncoder.encodeStringLiteral(" hello", ",");

            // Then
            assertEquals("\" hello\"", result);
        }

        @Test
        @DisplayName("should quote string with trailing space")
        void testTrailingSpace() {
            // Given
            String result = PrimitiveEncoder.encodeStringLiteral("hello ", ",");

            // Then
            assertEquals("\"hello \"", result);
        }

        @Test
        @DisplayName("should quote string with colon")
        void testColon() {
            // Given
            String result = PrimitiveEncoder.encodeStringLiteral("key:value", ",");

            // Then
            assertEquals("\"key:value\"", result);
        }

        @Test
        @DisplayName("should quote string with active delimiter")
        void testDelimiter() {
            // Given
            String result = PrimitiveEncoder.encodeStringLiteral("a,b,c", ",");

            // Then
            assertEquals("\"a,b,c\"", result);
        }

        @Test
        @DisplayName("should not quote string with inactive delimiter")
        void testInactiveDelimiter() {
            // Given
            String result = PrimitiveEncoder.encodeStringLiteral("a|b|c", ",");

            // Then
            assertEquals("a|b|c", result);
        }
    }

    @Nested
    @DisplayName("encodeKey")
    class EncodeKey {

        @Test
        @DisplayName("should encode simple key without quotes")
        void testSimpleKey() {
            // Given
            String result = PrimitiveEncoder.encodeKey("name");

            // Then
            assertEquals("name", result);
        }

        @Test
        @DisplayName("should encode key with underscores without quotes")
        void testKeyWithUnderscore() {
            // Given
            String result = PrimitiveEncoder.encodeKey("user_name");

            // Then
            assertEquals("user_name", result);
        }

        @Test
        @DisplayName("should encode key with dots without quotes")
        void testKeyWithDots() {
            // Given
            String result = PrimitiveEncoder.encodeKey("com.example.key");

            // Then
            assertEquals("com.example.key", result);
        }

        @Test
        @DisplayName("should quote key with spaces")
        void testKeyWithSpaces() {
            // Given
            String result = PrimitiveEncoder.encodeKey("full name");

            // Then
            assertEquals("\"full name\"", result);
        }

        @Test
        @DisplayName("should quote numeric key")
        void testNumericKey() {
            String result = PrimitiveEncoder.encodeKey("123");
            assertEquals("\"123\"", result);
        }

        @Test
        @DisplayName("should quote key starting with hyphen")
        void testKeyWithLeadingHyphen() {
            String result = PrimitiveEncoder.encodeKey("-key");
            assertEquals("\"-key\"", result);
        }

        @Test
        @DisplayName("should quote empty key")
        void testEmptyKey() {
            // Given
            String result = PrimitiveEncoder.encodeKey("");

            // Then
            assertEquals("\"\"", result);
        }

        @Test
        @DisplayName("should quote key with special characters")
        void testKeyWithSpecialChars() {
            // Given
            String result = PrimitiveEncoder.encodeKey("key:value");

            // Then
            assertEquals("\"key:value\"", result);
        }
    }

    @Nested
    @DisplayName("joinEncodedValues")
    class JoinEncodedValues {

        @Test
        @DisplayName("should join primitive values with comma")
        void testJoinWithComma() {
            // Given
            List<JsonNode> values = List.of(
                IntNode.valueOf(1),
                StringNode.valueOf("hello"),
                BooleanNode.TRUE);

            // When
            String result = PrimitiveEncoder.joinEncodedValues(values, ",");

            // Then
            assertEquals("1,hello,true", result);
        }

        @Test
        @DisplayName("should join values with pipe delimiter")
        void testJoinWithPipe() {
            // Given
            List<JsonNode> values = List.of(
                IntNode.valueOf(1),
                StringNode.valueOf("test"),
                IntNode.valueOf(2));

            // When
            String result = PrimitiveEncoder.joinEncodedValues(values, "|");

            // Then
            assertEquals("1|test|2", result);
        }

        @Test
        @DisplayName("should join values with tab delimiter")
        void testJoinWithTab() {
            // Given
            List<JsonNode> values = List.of(
                StringNode.valueOf("a"),
                StringNode.valueOf("b"),
                StringNode.valueOf("c"));

            // When
            String result = PrimitiveEncoder.joinEncodedValues(values, "\t");

            // Then
            assertEquals("a\tb\tc", result);
        }

        @Test
        @DisplayName("should handle empty list")
        void testEmptyList() {
            // Given
            List<JsonNode> values = List.of();

            // When
            String result = PrimitiveEncoder.joinEncodedValues(values, ",");

            // Then
            assertEquals("", result);
        }

        @Test
        @DisplayName("should handle single value")
        void testSingleValue() {
            // Given
            List<JsonNode> values = List.of(IntNode.valueOf(42));

            // When
            String result = PrimitiveEncoder.joinEncodedValues(values, ",");

            // Then
            assertEquals("42", result);
        }

        @Test
        @DisplayName("should quote values containing delimiter")
        void testQuoteDelimiter() {
            // Given
            List<JsonNode> values = List.of(
                StringNode.valueOf("a,b"),
                StringNode.valueOf("c,d"));

            // When
            String result = PrimitiveEncoder.joinEncodedValues(values, ",");

            // Then
            assertEquals("\"a,b\",\"c,d\"", result);
        }

        @Test
        @DisplayName("should handle null values")
        void testNullValues() {
            // Given
            List<JsonNode> values = List.of(
                IntNode.valueOf(1),
                NullNode.getInstance(),
                IntNode.valueOf(2));

            // When
            String result = PrimitiveEncoder.joinEncodedValues(values, ",");

            // Then
            assertEquals("1,null,2", result);
        }
    }

    @Nested
    @DisplayName("formatHeader")
    class FormatHeader {

        @Test
        @DisplayName("should format simple array header")
        void testSimpleHeader() {
            // Given
            String result = PrimitiveEncoder.formatHeader(5, "items", null, ",", false);

            // Then
            assertEquals("items[5]:", result);
        }

        @Test
        @DisplayName("should format tabular header")
        void testTabularHeader() {
            // Given
            List<String> fields = List.of("id", "name");

            // When
            String result = PrimitiveEncoder.formatHeader(3, "users", fields, ",", false);

            // Then
            assertEquals("users[3]{id,name}:", result);
        }

        @Test
        @DisplayName("should format header with length marker")
        void testWithLengthMarker() {
            // Given
            String result = PrimitiveEncoder.formatHeader(5, "data", null, ",", true);

            // Then
            assertEquals("data[#5]:", result);
        }

        @Test
        @DisplayName("should format header with pipe delimiter")
        void testPipeDelimiter() {
            // Given
            List<String> fields = List.of("x", "y");

            // When
            String result = PrimitiveEncoder.formatHeader(2, "points", fields, "|", false);

            // Then
            assertEquals("points[2|]{x|y}:", result);
        }

        @Test
        @DisplayName("should format header without key")
        void testWithoutKey() {
            // Given
            String result = PrimitiveEncoder.formatHeader(3, null, null, ",", false);

            // Then
            assertEquals("[3]:", result);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Integration")
    class EdgeCasesIntegration {

        @Test
        @DisplayName("should handle Unicode in string encoding")
        void testUnicode() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf("Hello 世界"), ",");

            // Then
            assertEquals("Hello 世界", result);
        }

        @Test
        @DisplayName("should handle emoji in string encoding")
        void testEmoji() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf("Hello 🌍"), ",");

            // Then
            assertEquals("Hello 🌍", result);
        }

        @Test
        @DisplayName("should handle complex escaped string")
        void testComplexEscaping() {
            // Given
            String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf("line1\nline2\ttab"), ",");

            // Then
            assertEquals("\"line1\\nline2\\ttab\"", result);
        }

        @Test
        @DisplayName("should join mixed type values correctly")
        void testMixedTypes() {
            // Given
            List<JsonNode> values = List.of(
                IntNode.valueOf(123),
                StringNode.valueOf("text"),
                BooleanNode.FALSE,
                NullNode.getInstance(),
                DoubleNode.valueOf(3.14));

            // When
            String result = PrimitiveEncoder.joinEncodedValues(values, ",");

            // Then
            assertEquals("123,text,false,null,3.14", result);
        }
    }
}

