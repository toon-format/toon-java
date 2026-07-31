package dev.toonformat.jtoon.encoder;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import dev.toonformat.jtoon.Delimiter;
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
            final String result = PrimitiveEncoder.encodePrimitive(BooleanNode.TRUE, Delimiter.COMMA.toString());

            // Then
            assertEquals("true", result);
        }

        @Test
        @DisplayName("should encode false")
        void testFalse() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(BooleanNode.FALSE, Delimiter.COMMA.toString());

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
            final String result = PrimitiveEncoder.encodePrimitive(IntNode.valueOf(42), Delimiter.COMMA.toString());

            // Then
            assertEquals("42", result);
        }

        @Test
        @DisplayName("should encode negative integer")
        void testNegativeInteger() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(IntNode.valueOf(-100), Delimiter.COMMA.toString());

            // Then
            assertEquals("-100", result);
        }

        @Test
        @DisplayName("should encode zero")
        void testZero() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(IntNode.valueOf(0), Delimiter.COMMA.toString());

            // Then
            assertEquals("0", result);
        }

        @Test
        @DisplayName("should encode long")
        void testLong() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                LongNode.valueOf(9999999999L), Delimiter.COMMA.toString());

            // Then
            assertEquals("9999999999", result);
        }

        @Test
        @DisplayName("should encode double")
        void testDouble() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                DoubleNode.valueOf(3.14), Delimiter.COMMA.toString());

            // Then
            assertEquals("3.14", result);
        }

        @Test
        @DisplayName("should encode float")
        void testFloat() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(FloatNode.valueOf(2.5f), Delimiter.COMMA.toString());

            // Then
            assertEquals("2.5", result);
        }

        @Test
        @DisplayName("should encode decimal node")
        void testDecimal() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                DecimalNode.valueOf(new java.math.BigDecimal("123.456")), Delimiter.COMMA.toString());

            // Then
            assertEquals("123.456", result);
        }

        @Test
        @DisplayName("should preserve high-precision BigDecimal exactly")
        void testHighPrecisionDecimal() {
            // Given — a 40-digit number that would lose precision through Double
            final java.math.BigDecimal precise = new java.math.BigDecimal(
                "1234567890123456789012345678901234567890.12345678901234567890");

            // When
            final String result = PrimitiveEncoder.encodePrimitive(
                DecimalNode.valueOf(precise), Delimiter.COMMA.toString());

            // Then — exact value preserved, not truncated via double precision
            // trailing zero stripped by stripTrailingZeros
            assertEquals("1234567890123456789012345678901234567890.1234567890123456789", result);
        }

        @Test
        @DisplayName("should preserve high-precision small decimal")
        void testHighPrecisionSmallDecimal() {
            // Given — a tiny fractional number that loses precision via Double
            final java.math.BigDecimal tiny = new java.math.BigDecimal("0.00000000000012345678901234567890");

            // When
            final String result = PrimitiveEncoder.encodePrimitive(
                DecimalNode.valueOf(tiny), Delimiter.COMMA.toString());

            // Then — trailing zero stripped by stripTrailingZeros
            assertEquals("0.0000000000001234567890123456789", result);
        }
    }

    @Nested
    @DisplayName("encodePrimitive - Strings")
    class EncodePrimitiveString {

        @Test
        @DisplayName("should encode simple string unquoted")
        void testSimpleString() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                StringNode.valueOf("hello"), Delimiter.COMMA.toString());

            // Then
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("should quote string with comma when using comma delimiter")
        void testStringWithComma() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                StringNode.valueOf("a,b"), Delimiter.COMMA.toString());

            // Then
            assertEquals("\"a,b\"", result);
        }

        @Test
        @DisplayName("should not quote string with comma when using pipe delimiter")
        void testStringWithCommaUsingPipe() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                StringNode.valueOf("a,b"), Delimiter.PIPE.toString());

            // Then
            assertEquals("a,b", result);
        }

        @Test
        @DisplayName("should quote empty string")
        void testEmptyString() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(StringNode.valueOf(""), Delimiter.COMMA.toString());

            // Then
            assertEquals("\"\"", result);
        }

        @Test
        @DisplayName("should quote string that looks like boolean")
        void testBooleanLikeString() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                StringNode.valueOf("true"), Delimiter.COMMA.toString());

            // Then
            assertEquals("\"true\"", result);
        }

        @Test
        @DisplayName("should quote string that looks like null")
        void testNullLikeString() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                StringNode.valueOf("null"), Delimiter.COMMA.toString());

            // Then
            assertEquals("\"null\"", result);
        }

        @Test
        @DisplayName("should quote string that looks like number")
        void testNumberLikeString() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                StringNode.valueOf("123"), Delimiter.COMMA.toString());

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
            final String result = PrimitiveEncoder.encodePrimitive(NullNode.getInstance(), Delimiter.COMMA.toString());

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
            final String result = PrimitiveEncoder.encodeStringLiteral("hello world", Delimiter.COMMA.toString());

            // Then
            assertEquals("hello world", result);
        }

        @Test
        @DisplayName("should quote and escape string with quotes")
        void testStringWithQuotes() {
            // Given
            final String result = PrimitiveEncoder.encodeStringLiteral("say \"hi\"", Delimiter.COMMA.toString());

            // Then
            assertEquals("\"say \\\"hi\\\"\"", result);
        }

        @Test
        @DisplayName("should quote string with leading space")
        void testLeadingSpace() {
            // Given
            final String result = PrimitiveEncoder.encodeStringLiteral(" hello", Delimiter.COMMA.toString());

            // Then
            assertEquals("\" hello\"", result);
        }

        @Test
        @DisplayName("should quote string with trailing space")
        void testTrailingSpace() {
            // Given
            final String result = PrimitiveEncoder.encodeStringLiteral("hello ", Delimiter.COMMA.toString());

            // Then
            assertEquals("\"hello \"", result);
        }

        @Test
        @DisplayName("should quote string with colon")
        void testColon() {
            // Given
            final String result = PrimitiveEncoder.encodeStringLiteral("key:value", Delimiter.COMMA.toString());

            // Then
            assertEquals("\"key:value\"", result);
        }

        @Test
        @DisplayName("should quote string with active delimiter")
        void testDelimiter() {
            // Given
            final String result = PrimitiveEncoder.encodeStringLiteral("a,b,c", Delimiter.COMMA.toString());

            // Then
            assertEquals("\"a,b,c\"", result);
        }

        @Test
        @DisplayName("should not quote string with inactive delimiter")
        void testInactiveDelimiter() {
            // Given
            final String result = PrimitiveEncoder.encodeStringLiteral("a|b|c", Delimiter.COMMA.toString());

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
            final String result = PrimitiveEncoder.encodeKey("name");

            // Then
            assertEquals("name", result);
        }

        @Test
        @DisplayName("should encode key with underscores without quotes")
        void testKeyWithUnderscore() {
            // Given
            final String result = PrimitiveEncoder.encodeKey("user_name");

            // Then
            assertEquals("user_name", result);
        }

        @Test
        @DisplayName("should encode key with dots without quotes")
        void testKeyWithDots() {
            // Given
            final String result = PrimitiveEncoder.encodeKey("com.example.key");

            // Then
            assertEquals("com.example.key", result);
        }

        @Test
        @DisplayName("should quote key with spaces")
        void testKeyWithSpaces() {
            // Given
            final String result = PrimitiveEncoder.encodeKey("full name");

            // Then
            assertEquals("\"full name\"", result);
        }

        @Test
        @DisplayName("should quote numeric key")
        void testNumericKey() {
            final String result = PrimitiveEncoder.encodeKey("123");
            assertEquals("\"123\"", result);
        }

        @Test
        @DisplayName("should quote key starting with hyphen")
        void testKeyWithLeadingHyphen() {
            final String result = PrimitiveEncoder.encodeKey("-key");
            assertEquals("\"-key\"", result);
        }

        @Test
        @DisplayName("should quote empty key")
        void testEmptyKey() {
            // Given
            final String result = PrimitiveEncoder.encodeKey("");

            // Then
            assertEquals("\"\"", result);
        }

        @Test
        @DisplayName("should quote key with special characters")
        void testKeyWithSpecialChars() {
            // Given
            final String result = PrimitiveEncoder.encodeKey("key:value");

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
            final List<JsonNode> values = List.of(
                IntNode.valueOf(1),
                StringNode.valueOf("hello"),
                BooleanNode.TRUE);

            // When
            final String result = PrimitiveEncoder.joinEncodedValues(values, Delimiter.COMMA.toString());

            // Then
            assertEquals("1,hello,true", result);
        }

        @Test
        @DisplayName("should join values with pipe delimiter")
        void testJoinWithPipe() {
            // Given
            final List<JsonNode> values = List.of(
                IntNode.valueOf(1),
                StringNode.valueOf("test"),
                IntNode.valueOf(2));

            // When
            final String result = PrimitiveEncoder.joinEncodedValues(values, Delimiter.PIPE.toString());

            // Then
            assertEquals("1|test|2", result);
        }

        @Test
        @DisplayName("should join values with tab delimiter")
        void testJoinWithTab() {
            // Given
            final List<JsonNode> values = List.of(
                StringNode.valueOf("a"),
                StringNode.valueOf("b"),
                StringNode.valueOf("c"));

            // When
            final String result = PrimitiveEncoder.joinEncodedValues(values, Delimiter.TAB.toString());

            // Then
            assertEquals("a\tb\tc", result);
        }

        @Test
        @DisplayName("should handle empty list")
        void testEmptyList() {
            // Given
            final List<JsonNode> values = List.of();

            // When
            final String result = PrimitiveEncoder.joinEncodedValues(values, Delimiter.COMMA.toString());

            // Then
            assertEquals("", result);
        }

        @Test
        @DisplayName("should handle single value")
        void testSingleValue() {
            // Given
            final List<JsonNode> values = List.of(IntNode.valueOf(42));

            // When
            final String result = PrimitiveEncoder.joinEncodedValues(values, Delimiter.COMMA.toString());

            // Then
            assertEquals("42", result);
        }

        @Test
        @DisplayName("should quote values containing delimiter")
        void testQuoteDelimiter() {
            // Given
            final List<JsonNode> values = List.of(
                StringNode.valueOf("a,b"),
                StringNode.valueOf("c,d"));

            // When
            final String result = PrimitiveEncoder.joinEncodedValues(values, Delimiter.COMMA.toString());

            // Then
            assertEquals("\"a,b\",\"c,d\"", result);
        }

        @Test
        @DisplayName("should handle null values")
        void testNullValues() {
            // Given
            final List<JsonNode> values = List.of(
                IntNode.valueOf(1),
                NullNode.getInstance(),
                IntNode.valueOf(2));

            // When
            final String result = PrimitiveEncoder.joinEncodedValues(values, Delimiter.COMMA.toString());

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
            final String result = PrimitiveEncoder.formatHeader(5, "items", null, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("items[5]:", result);
        }

        @Test
        @DisplayName("should format tabular header")
        void testTabularHeader() {
            // Given
            final List<TabularField> fields = List.of(TabularField.leaf("id"), TabularField.leaf("name"));

            // When
            final String result = PrimitiveEncoder.formatHeader(3, "users", fields, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("users[3]{id,name}:", result);
        }

        @Test
        @DisplayName("should format header with length marker")
        void testWithLengthMarker() {
            // Given
            final String result = PrimitiveEncoder.formatHeader(5, "data", null, Delimiter.COMMA.toString(), true);

            // Then
            assertEquals("data[#5]:", result);
        }

        @Test
        @DisplayName("should format header with pipe delimiter")
        void testPipeDelimiter() {
            // Given
            final List<TabularField> fields = List.of(TabularField.leaf("x"), TabularField.leaf("y"));

            // When
            final String result = PrimitiveEncoder.formatHeader(2, "points", fields, Delimiter.PIPE.toString(), false);

            // Then
            assertEquals("points[2|]{x|y}:", result);
        }

        @Test
        @DisplayName("should format header without key")
        void testWithoutKey() {
            // Given
            final String result = PrimitiveEncoder.formatHeader(3, null, null, Delimiter.COMMA.toString(), false);

            // Then
            assertEquals("[3]:", result);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Integration")
    class EdgeCasesIntegration {
//
        @Test
        @DisplayName("should handle Unicode in string encoding")
        void testUnicode() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                StringNode.valueOf("Hello 世界"), Delimiter.COMMA.toString());

            // Then
            assertEquals("Hello 世界", result);
        }

        @Test
        @DisplayName("should handle emoji in string encoding")
        void testEmoji() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                StringNode.valueOf("Hello 🌍"), Delimiter.COMMA.toString());

            // Then
            assertEquals("Hello 🌍", result);
        }

        @Test
        @DisplayName("should handle complex escaped string")
        void testComplexEscaping() {
            // Given
            final String result = PrimitiveEncoder.encodePrimitive(
                StringNode.valueOf("line1\nline2\ttab"), Delimiter.COMMA.toString());

            // Then
            assertEquals("\"line1\\nline2\\ttab\"", result);
        }

        @Test
        @DisplayName("should join mixed type values correctly")
        void testMixedTypes() {
            // Given
            final List<JsonNode> values = List.of(
                IntNode.valueOf(123),
                StringNode.valueOf("text"),
                BooleanNode.FALSE,
                NullNode.getInstance(),
                DoubleNode.valueOf(3.14));

            // When
            final String result = PrimitiveEncoder.joinEncodedValues(values, Delimiter.COMMA.toString());

            // Then
            assertEquals("123,text,false,null,3.14", result);
        }
    }
}

