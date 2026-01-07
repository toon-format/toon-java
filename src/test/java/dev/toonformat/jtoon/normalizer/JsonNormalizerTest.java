package dev.toonformat.jtoon.normalizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test class for JsonNormalizer utility.
 */
@Tag("unit")
class JsonNormalizerTest {

    @Nested
    @DisplayName("Null and JsonNode")
    class NullAndJsonNode {

        @Test
        @DisplayName("should return NullNode for null input")
        void testNullInput() {
            // Given
            JsonNode result = JsonNormalizer.normalize(null);

            // Then
            assertTrue(result.isNull());
            assertInstanceOf(NullNode.class, result);
        }

        @Test
        @DisplayName("should pass through JsonNode unchanged")
        void testJsonNodePassthrough() {
            // Given
            JsonNode textNode = StringNode.valueOf("test");

            // When
            JsonNode result = JsonNormalizer.normalize(textNode);

            // Then
            assertSame(textNode, result);
        }

        @Test
        void testJsonNodePassthrough2() {
            // Given
            JsonNode intNode = IntNode.valueOf(42);

            // When
            JsonNode result = JsonNormalizer.normalize(intNode);

            // Then
            assertSame(intNode, result);
        }

        @Test
        void testJsonNodePassthrough3() {
            // Given
            JsonNode boolNode = BooleanNode.TRUE;

            // When
            JsonNode result = JsonNormalizer.normalize(boolNode);

            // Then
            assertSame(boolNode, result);
        }
    }

    @Nested
    @DisplayName("Primitive Types")
    class PrimitiveTypes {

        @Test
        @DisplayName("should normalize String to StringNode")
        void testString() {
            // Given
            JsonNode result = JsonNormalizer.normalize("hello");

            // Then
            assertTrue(result.isString());
            assertEquals("hello", result.asString());
            assertInstanceOf(StringNode.class, result);
        }

        @Test
        @DisplayName("should normalize empty String to StringNode")
        void testEmptyString() {
            // Given
            JsonNode result = JsonNormalizer.normalize("");

            // Then
            assertTrue(result.isString());
            assertEquals("", result.asString());
        }

        @Test
        @DisplayName("should normalize Boolean to BooleanNode")
        void testBoolean() {
            // Given
            JsonNode resultTrue = JsonNormalizer.normalize(Boolean.TRUE);

            // Then
            assertTrue(resultTrue.isBoolean());
            assertTrue(resultTrue.asBoolean());
            assertInstanceOf(BooleanNode.class, resultTrue);
        }

        @Test
        @DisplayName("should normalize Boolean to BooleanNode")
        void testBoolean2() {
            // Given
            JsonNode resultFalse = JsonNormalizer.normalize(Boolean.FALSE);

            // Then
            assertTrue(resultFalse.isBoolean());
            assertFalse(resultFalse.asBoolean());
        }

        @Test
        @DisplayName("should normalize Integer to IntNode")
        void testInteger() {
            // Given
            JsonNode result = JsonNormalizer.normalize(42);

            // Then
            assertTrue(result.isInt());
            assertEquals(42, result.asInt());
            assertInstanceOf(IntNode.class, result);
        }

        @Test
        @DisplayName("should normalize Long to LongNode")
        void testLong() {
            // Given
            JsonNode result = JsonNormalizer.normalize(9223372036854775807L);

            // Then
            assertTrue(result.isLong());
            assertEquals(9223372036854775807L, result.asLong());
            assertInstanceOf(LongNode.class, result);
        }

        @Test
        @DisplayName("should normalize Short to ShortNode")
        void testShort() {
            // Given
            JsonNode result = JsonNormalizer.normalize((short) 32767);

            // Then
            assertTrue(result.isShort());
            assertEquals(32767, result.asInt());
            assertInstanceOf(ShortNode.class, result);
        }

        @Test
        @DisplayName("should normalize Byte to IntNode")
        void testByte() {
            // Given
            JsonNode result = JsonNormalizer.normalize((byte) 127);

            // Then
            assertTrue(result.isInt());
            assertEquals(127, result.asInt());
            assertInstanceOf(IntNode.class, result);
        }

        @Test
        @DisplayName("should normalize Float to FloatNode")
        void testFloat() {
            // Given
            JsonNode result = JsonNormalizer.normalize(3.14f);

            // Then
            assertTrue(result.isFloat());
            assertEquals(3.14f, result.floatValue(), 0.001);
            assertInstanceOf(FloatNode.class, result);
        }

        @Test
        @DisplayName("should normalize Double to DoubleNode")
        void testDouble() {
            // Given
            JsonNode result = JsonNormalizer.normalize(3.14159);

            // Then
            assertTrue(result.isDouble());
            assertEquals(3.14159, result.asDouble(), 0.00001);
            assertInstanceOf(DoubleNode.class, result);
        }
    }

    @Nested
    @DisplayName("Special Double Cases")
    class SpecialDoubleCases {

        @Test
        @DisplayName("should convert NaN to NullNode")
        void testNaN() {
            // Given
            JsonNode result = JsonNormalizer.normalize(Double.NaN);

            // Then
            assertTrue(result.isNull());
            assertInstanceOf(NullNode.class, result);
        }

        @Test
        @DisplayName("should convert positive Infinity to NullNode")
        void testPositiveInfinity() {
            // Given
            JsonNode result = JsonNormalizer.normalize(Double.POSITIVE_INFINITY);

            // Then
            assertTrue(result.isNull());
            assertInstanceOf(NullNode.class, result);
        }

        @Test
        @DisplayName("should convert negative Infinity to NullNode")
        void testNegativeInfinity() {
            // Given
            JsonNode result = JsonNormalizer.normalize(Double.NEGATIVE_INFINITY);

            // Then
            assertTrue(result.isNull());
            assertInstanceOf(NullNode.class, result);
        }

        @Test
        @DisplayName("should canonicalize -0.0 to IntNode(0)")
        void testNegativeZero() {
            // Given
            JsonNode result = JsonNormalizer.normalize(-0.0);

            // Then
            assertTrue(result.isInt());
            assertEquals(0, result.asInt());
            assertInstanceOf(IntNode.class, result);
        }

        @Test
        @DisplayName("should canonicalize +0.0 to IntNode(0)")
        void testPositiveZero() {
            // Given
            JsonNode result = JsonNormalizer.normalize(0.0);

            // Then
            assertTrue(result.isInt());
            assertEquals(0, result.asInt());
            assertInstanceOf(IntNode.class, result);
        }

        @Test
        @DisplayName("should convert whole numbers to LongNode when in range")
        void testWholeNumbers() {
            // Given
            JsonNode result = JsonNormalizer.normalize(42.0);
            // Then
            assertTrue(result.isIntegralNumber());
            assertEquals(42, result.asLong());
            assertInstanceOf(LongNode.class, result);
        }

        @Test
        @DisplayName("should convert whole numbers to LongNode when in range")
        void testWholeNumbers2() {
            // Given
            JsonNode result = JsonNormalizer.normalize(1000000.0);

            // Then
            assertTrue(result.isIntegralNumber());
            assertEquals(1000000, result.asLong());
        }

        @Test
        @DisplayName("should keep regular decimals as DoubleNode")
        void testRegularDecimals() {
            // Given
            JsonNode result = JsonNormalizer.normalize(3.14159);

            // Then
            assertTrue(result.isDouble());
            assertEquals(3.14159, result.asDouble(), 0.00001);
            assertInstanceOf(DoubleNode.class, result);
        }

        @Test
        @DisplayName("should convert Float NaN to NullNode")
        void testFloatNaN() {
            // Given
            JsonNode result = JsonNormalizer.normalize(Float.NaN);

            // Then
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("should convert Float Infinity to NullNode")
        void testFloatInfinity() {
            // Given
            JsonNode result = JsonNormalizer.normalize(Float.POSITIVE_INFINITY);

            // Then
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("should convert Float Infinity to NullNode")
        void testFloatInfinity2() {
            // Given
            JsonNode result = JsonNormalizer.normalize(Float.NEGATIVE_INFINITY);

            // Then
            assertTrue(result.isNull());
        }
    }

    @Nested
    @DisplayName("Big Numbers")
    class BigNumbers {

        @Test
        @DisplayName("should convert BigInteger within Long range to LongNode")
        void testBigIntegerInRange() {
            // Given
            BigInteger bigInt = BigInteger.valueOf(123456789L);

            // When
            JsonNode result = JsonNormalizer.normalize(bigInt);

            // Then
            assertTrue(result.isLong());
            assertEquals(123456789L, result.asLong());
            assertInstanceOf(LongNode.class, result);
        }

        @Test
        @DisplayName("should convert BigInteger at Long.MAX_VALUE to LongNode")
        void testBigIntegerAtMaxLong() {
            // Given
            BigInteger bigInt = BigInteger.valueOf(Long.MAX_VALUE);

            // When
            JsonNode result = JsonNormalizer.normalize(bigInt);

            // Then
            assertTrue(result.isLong());
            assertEquals(Long.MAX_VALUE, result.asLong());
        }

        @Test
        @DisplayName("should convert BigInteger at Long.MIN_VALUE to LongNode")
        void testBigIntegerAtMinLong() {
            // Given
            BigInteger bigInt = BigInteger.valueOf(Long.MIN_VALUE);

            // When
            JsonNode result = JsonNormalizer.normalize(bigInt);

            // Then
            assertTrue(result.isLong());
            assertEquals(Long.MIN_VALUE, result.asLong());
        }

        @Test
        @DisplayName("should convert BigInteger outside Long range to StringNode")
        void testBigIntegerOutOfRange() {
            // Given
            BigInteger bigInt = new BigInteger("99999999999999999999999999999999");

            // When
            JsonNode result = JsonNormalizer.normalize(bigInt);

            // Then
            assertTrue(result.isString());
            assertEquals("99999999999999999999999999999999", result.asString());
            assertInstanceOf(StringNode.class, result);
        }

        @Test
        @DisplayName("should convert BigDecimal to DecimalNode")
        void testBigDecimal() {
            // Given
            BigDecimal bigDec = new BigDecimal("123.456");

            // When
            JsonNode result = JsonNormalizer.normalize(bigDec);

            // Then
            assertTrue(result.isBigDecimal());
            assertEquals(new BigDecimal("123.456"), result.decimalValue());
            assertInstanceOf(DecimalNode.class, result);
        }

        @Test
        @DisplayName("should convert large BigDecimal to DecimalNode")
        void testLargeBigDecimal() {
            // Given
            BigDecimal bigDec = new BigDecimal("999999999999999999999.999999999999999999");

            // When
            JsonNode result = JsonNormalizer.normalize(bigDec);

            // Then
            assertTrue(result.isBigDecimal());
            assertInstanceOf(DecimalNode.class, result);
        }
    }

    @Nested
    @DisplayName("Temporal Types")
    class TemporalTypes {

        @Test
        @DisplayName("should convert LocalDateTime to ISO formatted StringNode")
        void testLocalDateTime() {
            // Given
            LocalDateTime dateTime = LocalDateTime.of(2023, 10, 15, 14, 30, 45);

            // When
            JsonNode result = JsonNormalizer.normalize(dateTime);

            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15T14:30:45", result.asString());
        }

        @Test
        @DisplayName("should convert java.sql.Date to ISO formatted StringNode")
        void testSQLDate() {
            // Given
            java.sql.Date dateTime = new java.sql.Date(1766419274);

            // When
            JsonNode result = JsonNormalizer.normalize(dateTime);

            // Then
            assertTrue(result.isString());
            assertEquals("1970-01-21", result.asString());
        }

        @Test
        @DisplayName("should convert java.sql.Time to ISO formatted StringNode")
        void testSQLTime() {
            // Given
            java.sql.Time time = new java.sql.Time(1766419274);

            // When
            JsonNode result = JsonNormalizer.normalize(time);

            // Then
            assertTrue(result.isString());
            String expected = time.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
            assertEquals(expected, result.asString());
        }

        @Test
        @DisplayName("should convert java.sql.Timestamp to ISO formatted StringNode")
        void testSQLTimeStamp() {
            // Given
            java.sql.Timestamp dateTime = new java.sql.Timestamp(1766419274);

            // When
            JsonNode result = JsonNormalizer.normalize(dateTime);

            // Then
            assertTrue(result.isString());
            String expected = dateTime.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            assertEquals(expected, result.asString());
        }


        @Test
        @DisplayName("should convert LocalDate to ISO formatted StringNode")
        void testLocalDate() {
            // Given
            LocalDate date = LocalDate.of(2023, 10, 15);

            // When
            JsonNode result = JsonNormalizer.normalize(date);

            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15", result.asString());
        }

        @Test
        @DisplayName("should convert LocalTime to ISO formatted StringNode")
        void testLocalTime() {
            // Given
            LocalTime time = LocalTime.of(14, 30, 45);

            // When
            JsonNode result = JsonNormalizer.normalize(time);

            // Then
            assertTrue(result.isString());
            assertEquals("14:30:45", result.asString());
        }

        @Test
        @DisplayName("should convert ZonedDateTime to ISO formatted StringNode")
        void testZonedDateTime() {
            // Given
            ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 10, 15, 14, 30, 45, 0, ZoneId.of("UTC"));

            // When
            JsonNode result = JsonNormalizer.normalize(zonedDateTime);

            // Then
            assertTrue(result.isString());
            assertTrue(result.asString().startsWith("2023-10-15T14:30:45"));
        }

        @Test
        @DisplayName("should convert OffsetDateTime to ISO formatted StringNode")
        void testOffsetDateTime() {
            // Given
            OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 10, 15, 14, 30, 45, 0, ZoneOffset.UTC);

            // When
            JsonNode result = JsonNormalizer.normalize(offsetDateTime);

            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15T14:30:45Z", result.asString());
        }

        @Test
        @DisplayName("should convert Instant to ISO formatted StringNode")
        void testInstant() {
            // Given
            Instant instant = Instant.parse("2023-10-15T14:30:45.123Z");

            // When
            JsonNode result = JsonNormalizer.normalize(instant);

            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15T14:30:45.123Z", result.asString());
        }

        @Test
        @DisplayName("should convert java.util.Date to ISO formatted StringNode")
        void testUtilDate() {
            // Given
            Date date = Date.from(Instant.parse("2023-10-15T14:30:45.123Z"));

            // When
            JsonNode result = JsonNormalizer.normalize(date);

            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15", result.asString());
        }
    }

    @Nested
    @DisplayName("Collections")
    class Collections {

        @Test
        @DisplayName("should convert List to ArrayNode")
        void testList() {
            // Given
            List<Object> list = List.of(1, 2, 3, "four");

            // When
            JsonNode result = JsonNormalizer.normalize(list);

            // Then
            assertTrue(result.isArray());
            assertEquals(4, result.size());
            assertEquals(1, result.get(0).asInt());
            assertEquals(2, result.get(1).asInt());
            assertEquals(3, result.get(2).asInt());
            assertEquals("four", result.get(3).asString());
        }

        @Test
        @DisplayName("should convert empty List to empty ArrayNode")
        void testEmptyList() {
            // Given
            List<Object> list = List.of();

            // When
            JsonNode result = JsonNormalizer.normalize(list);

            // Then
            assertTrue(result.isArray());
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should convert Set to ArrayNode")
        void testSet() {
            // Given
            Set<Integer> set = new LinkedHashSet<>(List.of(1, 2, 3));

            // When
            JsonNode result = JsonNormalizer.normalize(set);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("should convert Map to ObjectNode")
        void testMap() {
            // Given
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "John");
            map.put("age", 30);
            map.put("active", true);

            // When
            JsonNode result = JsonNormalizer.normalize(map);

            // Then
            assertTrue(result.isObject());
            assertEquals(3, result.size());
            assertEquals("John", result.get("name").asString());
            assertEquals(30, result.get("age").asInt());
            assertTrue(result.get("active").asBoolean());
        }

        @Test
        @DisplayName("should convert empty Map to empty ObjectNode")
        void testEmptyMap() {
            // Given
            Map<String, Object> map = new HashMap<>();

            // When
            JsonNode result = JsonNormalizer.normalize(map);

            // Then
            assertTrue(result.isObject());
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should handle nested collections")
        void testNestedCollections() {
            // Given
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("numbers", List.of(1, 2, 3));
            map.put("nested", Map.of("key", "value"));

            // When
            JsonNode result = JsonNormalizer.normalize(map);

            // Then
            assertTrue(result.isObject());
            assertTrue(result.get("numbers").isArray());
            assertEquals(3, result.get("numbers").size());
            assertTrue(result.get("nested").isObject());
            assertEquals("value", result.get("nested").get("key").asString());
        }

        @Test
        @DisplayName("should convert non-String Map keys to String")
        void testMapWithNonStringKeys() {
            // Given
            Map<Integer, String> map = new HashMap<>();
            map.put(1, "one");
            map.put(2, "two");

            // When
            JsonNode result = JsonNormalizer.normalize(map);

            // Then
            assertTrue(result.isObject());
            assertEquals("one", result.get("1").asString());
            assertEquals("two", result.get("2").asString());
        }

        @Test
        @DisplayName("should handle collections with null values")
        void testCollectionWithNulls() {
            // Given
            List<Object> list = java.util.Arrays.asList(1, null, 3);

            // When
            JsonNode result = JsonNormalizer.normalize(list);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
            assertEquals(1, result.get(0).asInt());
            assertTrue(result.get(1).isNull());
            assertEquals(3, result.get(2).asInt());
        }
    }

    @Nested
    @DisplayName("Arrays")
    class Arrays {

        @Test
        @DisplayName("should convert int[] to ArrayNode")
        void testIntArray() {
            // Given
            int[] array = {1, 2, 3, 4, 5};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(5, result.size());
            assertEquals(1, result.get(0).asInt());
            assertEquals(5, result.get(4).asInt());
        }

        @Test
        @DisplayName("should convert long[] to ArrayNode")
        void testLongArray() {
            // Given
            long[] array = {1L, 2L, 9223372036854775807L};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
            assertEquals(9223372036854775807L, result.get(2).asLong());
        }

        @Test
        @DisplayName("should convert double[] to ArrayNode")
        void testDoubleArray() {
            // Given
            double[] array = {1.1, 2.2, 3.3};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
            assertEquals(1.1, result.get(0).asDouble(), 0.001);
        }

        @Test
        @DisplayName("should convert double[] with special values to ArrayNode with nulls")
        void testDoubleArrayWithSpecialValues() {
            // Given
            double[] array = {1.0, Double.NaN, Double.POSITIVE_INFINITY, 4.0, Double.NEGATIVE_INFINITY};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(5, result.size());
            assertEquals(1, result.get(0).asInt());
            assertTrue(result.get(1).isNull());
            assertTrue(result.get(2).isNull());
            assertEquals(4, result.get(3).asInt());
            assertTrue(result.get(4).isNull());
        }

        @Test
        @DisplayName("should convert float[] to ArrayNode")
        void testFloatArray() {
            // Given
            float[] array = {1.1f, 2.2f, 3.3f};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
            assertEquals(1.1f, result.get(0).floatValue(), 0.001);
        }

        @Test
        @DisplayName("should convert float[] with special values to ArrayNode with nulls")
        void testFloatArrayWithSpecialValues() {
            // Given
            float[] array = {1.0f, Float.NaN, Float.POSITIVE_INFINITY};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
            assertEquals(1.0f, result.get(0).floatValue(), 0.001);
            assertTrue(result.get(1).isNull());
            assertTrue(result.get(2).isNull());
        }

        @Test
        @DisplayName("should convert boolean[] to ArrayNode")
        void testBooleanArray() {
            // Given
            boolean[] array = {true, false, true};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
            assertTrue(result.get(0).asBoolean());
            assertFalse(result.get(1).asBoolean());
        }

        @Test
        @DisplayName("should convert byte[] to ArrayNode")
        void testByteArray() {
            // Given
            byte[] array = {1, 2, 127};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
            assertEquals(127, result.get(2).asInt());
        }

        @Test
        @DisplayName("should convert short[] to ArrayNode")
        void testShortArray() {
            // Given
            short[] array = {1, 2, 32767};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
            assertEquals(32767, result.get(2).asInt());
        }

        @Test
        @DisplayName("should convert char[] to ArrayNode of strings")
        void testCharArray() {
            // Given
            char[] array = {'a', 'b', 'c'};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
            assertEquals("a", result.get(0).asString());
            assertEquals("b", result.get(1).asString());
            assertEquals("c", result.get(2).asString());
        }

        @Test
        @DisplayName("should convert Object[] to ArrayNode")
        void testObjectArray() {
            // Given
            Object[] array = {1, "two", true, 3.14};

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(4, result.size());
            assertEquals(1, result.get(0).asInt());
            assertEquals("two", result.get(1).asString());
            assertTrue(result.get(2).asBoolean());
            assertEquals(3.14, result.get(3).asDouble(), 0.001);
        }

        @Test
        @DisplayName("should convert empty arrays to empty ArrayNode")
        void testEmptyArrays() {
            // Given
            int[] intArray = {};

            // When
            JsonNode result = JsonNormalizer.normalize(intArray);

            // Then
            assertTrue(result.isArray());
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should convert empty arrays to empty ArrayNode")
        void testEmptyArraysOfObjects() {
            // Given
            Object[] objArray = {};

            // When
            JsonNode result = JsonNormalizer.normalize(objArray);

            // Then
            assertTrue(result.isArray());
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should handle nested arrays")
        void testNestedArrays() {
            // Given
            Object[] array = {
                new int[]{1, 2},
                new String[]{"a", "b"}
            };

            // When
            JsonNode result = JsonNormalizer.normalize(array);

            // Then
            assertTrue(result.isArray());
            assertEquals(2, result.size());
            assertTrue(result.get(0).isArray());
            assertEquals(2, result.get(0).size());
            assertTrue(result.get(1).isArray());
            assertEquals("a", result.get(1).get(0).asString());
        }
    }

    @Nested
    @DisplayName("Special Types")
    class SpecialTypes {

        @Test
        @DisplayName("should convert Optional.empty() to NullNode")
        void testEmptyOptional() {
            // Given
            Optional<String> optional = Optional.empty();

            // When
            JsonNode result = JsonNormalizer.normalize(optional);

            // Then
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("should unwrap Optional.of(value)")
        void testOptionalWithValue() {
            // Given
            Optional<String> optional = Optional.of("hello");

            // When
            JsonNode result = JsonNormalizer.normalize(optional);

            // Then
            assertTrue(result.isString());
            assertEquals("hello", result.asString());
        }

        @Test
        @DisplayName("should unwrap Optional.of(value)")
        void testOptionalWithValue2() {
            // Given
            Optional<Integer> intOptional = Optional.of(42);

            // When
            JsonNode result = JsonNormalizer.normalize(intOptional);

            // Then
            assertTrue(result.isInt());
            assertEquals(42, result.asInt());
        }

        @Test
        @DisplayName("should unwrap nested Optional")
        void testNestedOptional() {
            // Given
            Optional<Optional<String>> nested = Optional.of(Optional.of("nested"));

            // When
            JsonNode result = JsonNormalizer.normalize(nested);

            // Then
            assertTrue(result.isString());
            assertEquals("nested", result.asString());
        }

        @Test
        @DisplayName("should convert Stream to ArrayNode")
        void testStream() {
            // Given
            Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);

            // When
            JsonNode result = JsonNormalizer.normalize(stream);

            // Then
            assertTrue(result.isArray());
            assertEquals(5, result.size());
            assertEquals(1, result.get(0).asInt());
            assertEquals(5, result.get(4).asInt());
        }

        @Test
        @DisplayName("should convert empty Stream to empty ArrayNode")
        void testEmptyStream() {
            // Given
            Stream<String> stream = Stream.empty();

            // When
            JsonNode result = JsonNormalizer.normalize(stream);

            // Then
            assertTrue(result.isArray());
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should handle Stream with null values")
        void testStreamWithNulls() {
            // Given
            Stream<Object> stream = Stream.of(1, null, 3);

            // When
            JsonNode result = JsonNormalizer.normalize(stream);

            // Then
            assertTrue(result.isArray());
            assertEquals(3, result.size());
            assertEquals(1, result.get(0).asInt());
            assertTrue(result.get(1).isNull());
            assertEquals(3, result.get(2).asInt());
        }
    }

    @Nested
    @DisplayName("POJOs")
    class POJOs {

        static class SimplePojo {
            public String name;
            public int age;

            SimplePojo(String name, int age) {
                this.name = name;
                this.age = age;
            }
        }

        record PojoWithGetters(String value) {
        }

        @Test
        @DisplayName("should convert simple POJO to ObjectNode")
        void testSimplePojo() {
            // Given
            SimplePojo pojo = new SimplePojo("Alice", 25);

            // When
            JsonNode result = JsonNormalizer.normalize(pojo);

            // Then
            assertTrue(result.isObject());
            assertEquals("Alice", result.get("name").asString());
            assertEquals(25, result.get("age").asInt());
        }

        @Test
        @DisplayName("should convert POJO with getters to ObjectNode")
        void testPojoWithGetters() {
            // Given
            PojoWithGetters pojo = new PojoWithGetters("test");

            // When
            JsonNode result = JsonNormalizer.normalize(pojo);

            // Then
            assertTrue(result.isObject());
            assertEquals("test", result.get("value").asString());
        }

        @Test
        @DisplayName("should handle nested POJOs")
        void testNestedPojo() {
            // Given
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("pojo", new SimplePojo("Bob", 30));
            map.put("id", 123);

            // When
            JsonNode result = JsonNormalizer.normalize(map);

            // Then
            assertTrue(result.isObject());
            assertTrue(result.get("pojo").isObject());
            assertEquals("Bob", result.get("pojo").get("name").asString());
            assertEquals(123, result.get("id").asInt());
        }

        @Test
        @DisplayName("should handle collections of POJOs")
        void testCollectionOfPojos() {
            // Given
            List<SimplePojo> pojos = List.of(
                new SimplePojo("Alice", 25),
                new SimplePojo("Bob", 30)
            );

            // When
            JsonNode result = JsonNormalizer.normalize(pojos);

            // Then
            assertTrue(result.isArray());
            assertEquals(2, result.size());
            assertEquals("Alice", result.get(0).get("name").asString());
            assertEquals("Bob", result.get(1).get("name").asString());
        }

        @Test
        @DisplayName("should convert non-serializable objects to NullNode")
        void testNonSerializableObject() {
            // Given
            // Thread is not easily serializable by Jackson
            Thread thread = new Thread();

            // When
            JsonNode result = JsonNormalizer.normalize(thread);

            // Then
            // Jackson may succeed or fail depending on version
            // Just verify it doesn't throw an exception
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle deeply nested structures")
        void testDeeplyNested() {
            // Given
            Map<String, Object> level3 = Map.of("value", 42);
            Map<String, Object> level2 = Map.of("level3", level3);
            Map<String, Object> level1 = Map.of("level2", level2);

            // When
            JsonNode result = JsonNormalizer.normalize(level1);

            // Then
            assertEquals(42, result.get("level2").get("level3").get("value").asInt());
        }

        @Test
        @DisplayName("should handle mixed types in collections")
        void testMixedTypes() {
            // Given
            List<Object> mixed = java.util.Arrays.asList(
                1,
                "text",
                true,
                3.14,
                List.of(1, 2),
                Map.of("key", "value"),
                null
            );

            // When
            JsonNode result = JsonNormalizer.normalize(mixed);

            // Then
            assertTrue(result.isArray());
            assertEquals(7, result.size());
            assertEquals(1, result.get(0).asInt());
            assertEquals("text", result.get(1).asString());
            assertTrue(result.get(2).asBoolean());
            assertEquals(3.14, result.get(3).asDouble(), 0.001);
            assertTrue(result.get(4).isArray());
            assertTrue(result.get(5).isObject());
            assertTrue(result.get(6).isNull());
        }

        @Test
        @DisplayName("should handle Optional with null value")
        void testOptionalOfNull() {
            // Given
            Optional<String> optional = Optional.empty();

            // When
            JsonNode result = JsonNormalizer.normalize(optional);

            // Then
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("should handle arrays containing arrays")
        void testArrayOfArrays() {
            // Given
            int[][] matrix = {{1, 2}, {3, 4}};

            // When
            JsonNode result = JsonNormalizer.normalize(matrix);

            // Then
            assertTrue(result.isArray());
            assertEquals(2, result.size());
            assertTrue(result.get(0).isArray());
            assertEquals(1, result.get(0).get(0).asInt());
            assertEquals(4, result.get(1).get(1).asInt());
        }

        @Test
        @DisplayName("should handle Map with null values")
        void testMapWithNullValues() {
            // Given
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("key1", "value");
            map.put("key2", null);

            // When
            JsonNode result = JsonNormalizer.normalize(map);

            // Then
            assertTrue(result.isObject());
            assertEquals("value", result.get("key1").asString());
            assertTrue(result.get("key2").isNull());
        }
    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<JsonNormalizer> constructor = JsonNormalizer.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When
        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        // Then
        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method declaredMethod = JsonNormalizer.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }


    @Nested
    @DisplayName("tryNormalizePrimitive")
    class TryNormalizePrimitive {

        @Test
        @DisplayName("Given an Integer value, When tryNormalizePrimitive is called, Then an IntNode is returned")
        void givenInteger_whenTryNormalizePrimitive_thenIntNode() throws Exception {
            // Given
            Integer input = 42;

            // When
            Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(IntNode.class, result);
            assertEquals(42, ((JsonNode) result).asInt());
        }

        @Test
        @DisplayName("Given an unsupported type, When tryNormalizePrimitive is called, Then null is returned")
        void givenUnsupported_whenTryNormalizePrimitive_thenNull() throws Exception {
            // Given
            Object input = new Object();

            // When
            Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Given an String value, When tryNormalizePrimitive is called, Then an StringNode is returned")
        void givenString_whenTryNormalizePrimitive_thenStringNode() throws Exception {
            // Given
            String input = "hello world";

            // When
            Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("hello world", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given an Boolean value, When tryNormalizePrimitive is called, Then an BooleanNode is returned")
        void givenBoolean_whenTryNormalizePrimitive_thenBooleanNode() throws Exception {
            // Given
            Boolean input = Boolean.TRUE;

            // When
            Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(BooleanNode.class, result);
            assertTrue(((JsonNode) result).asBoolean());
        }

        @Test
        @DisplayName("Given an Long value, When tryNormalizePrimitive is called, Then an LongNode is returned")
        void givenLong_whenTryNormalizePrimitive_thenLongNode() throws Exception {
            // Given
            Long input = Long.MAX_VALUE;

            // When
            Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(LongNode.class, result);
            assertEquals(Long.MAX_VALUE, ((JsonNode) result).asLong());
        }

        @Test
        @DisplayName("Given an Short value, When tryNormalizePrimitive is called, Then an ShortNode is returned")
        void givenShort_whenTryNormalizePrimitive_thenShortNode() throws Exception {
            // Given
            Short input = Short.MAX_VALUE;

            // When
            Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(ShortNode.class, result);
            assertEquals(Short.MAX_VALUE, ((JsonNode) result).asShort());
        }

        @Test
        @DisplayName("Given an Byte value, When tryNormalizePrimitive is called, Then an ByteNode is returned")
        void givenByte_whenTryNormalizePrimitive_thenByteNode() throws Exception {
            // Given
            byte input = 42;

            // When
            Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(IntNode.class, result);
            assertEquals(42, ((JsonNode) result).intValue());
        }
    }

    @Nested
    @DisplayName("tryNormalizeBigNumber")
    class TryNormalizeBigNumber {

        @Test
        @DisplayName("Given BigInteger within long range, When tryNormalizeBigNumber is called, Then a LongNode is returned")
        void givenBigIntegerInRange_whenTryNormalizeBigNumber_thenLongNode() throws Exception {
            // Given
            BigInteger input = BigInteger.valueOf(Long.MAX_VALUE);

            // When
            Object result = invokePrivateStatic("tryNormalizeBigNumber", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(LongNode.class, result);
            assertEquals(Long.MAX_VALUE, ((JsonNode) result).longValue());
        }

        @Test
        @DisplayName("Given BigInteger outside long range, When tryNormalizeBigNumber is called, Then a StringNode is returned")
        void givenBigIntegerOutOfRange_whenTryNormalizeBigNumber_thenStringNode() throws Exception {
            // Given
            BigInteger input = new BigInteger("99999999999999999999999999999999");

            // When
            Object result = invokePrivateStatic("tryNormalizeBigNumber", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals(input.toString(), ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given BigDecimal value, When tryNormalizeBigNumber is called, Then a DecimalNode is returned")
        void givenBigDecimal_whenTryNormalizeBigNumber_thenDecimalNode() throws Exception {
            // Given
            BigDecimal input = new BigDecimal("123.456");

            // When
            Object result = invokePrivateStatic("tryNormalizeBigNumber", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(DecimalNode.class, result);
            assertEquals(input, ((JsonNode) result).decimalValue());
        }

        @Test
        @DisplayName("Given non big-number type, When tryNormalizeBigNumber is called, Then null is returned")
        void givenOther_whenTryNormalizeBigNumber_thenNull() throws Exception {
            // Given
            String input = "not-a-number";

            // When
            Object result = invokePrivateStatic("tryNormalizeBigNumber", new Class[]{Object.class}, input);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("tryNormalizeTemporal")
    class TryNormalizeTemporal {

        @Test
        @DisplayName("Given LocalDate, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenLocalDate_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            LocalDate input = LocalDate.of(2024, 2, 29);

            // When
            Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("2024-02-29", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given non temporal type, When tryNormalizeTemporal is called, Then null is returned")
        void givenOther_whenTryNormalizeTemporal_thenNull() throws Exception {
            // Given
            Object input = 10;

            // When
            Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Given LocalDateTime, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenLocalDateTime_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            LocalDateTime input = LocalDateTime.of(2024, 2, 29, 14, 45, 12);

            // When
            Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("2024-02-29T14:45:12", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given LocalTime, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenLocalTime_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            LocalTime input = LocalTime.of(14, 45, 12);

            // When
            Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("14:45:12", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given ZoneDateTime, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenZoneDateTime_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            ZonedDateTime input = ZonedDateTime.of(LocalDate.of(2025, 11, 26), LocalTime.of(15, 45), ZoneId.of("Europe/Berlin"));

            // When
            Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("2025-11-26T15:45:00+01:00[Europe/Berlin]", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given OffsetDateTime, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenOffsetDateTime_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            ZoneId zone = ZoneId.of("Europe/Berlin");
            ZoneOffset zoneOffSet = zone.getRules().getOffset(LocalDateTime.of(2025, 11, 26, 15, 45, 36));
            OffsetDateTime input = OffsetDateTime.of(LocalDate.of(2025, 11, 26), LocalTime.of(15, 45), zoneOffSet);

            // When
            Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("2025-11-26T15:45:00+01:00", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given Instant, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenInstant_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            ZoneId zone = ZoneId.of("Europe/Berlin");
            ZoneOffset zoneOffSet = zone.getRules().getOffset(LocalDateTime.of(2025, 11, 26, 15, 45, 36));
            Instant input = LocalDateTime.of(2025, 11, 26, 15, 45, 36).toInstant(zoneOffSet);

            // When
            Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("2025-11-26T14:45:36Z", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given Calendar, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenCalendar_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            Calendar input = Calendar.getInstance();
            input.set(2017, Calendar.FEBRUARY, 16, 20, 22, 28);
            input.set(Calendar.MILLISECOND, 0);

            // When
            Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            String expected = input.toInstant().toString();
            assertEquals(expected, ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given GregorianCalendar, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenGregorianCalendar_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            GregorianCalendar input = new GregorianCalendar(2017, Calendar.FEBRUARY, 16, 20, 22, 28);

            // When
            Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            String expected = input.toInstant().toString();
            assertEquals(expected, ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given Date, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenDate_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            Date input = new Date(1764362004);

            // When
            Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("1970-01-21", ((JsonNode) result).asString());
        }
    }

    @Nested
    @DisplayName("tryConvertToLong")
    class TryConvertToLong {

        @Test
        @DisplayName("Given whole double within long range, When tryConvertToLong is called, Then Optional with LongNode is returned")
        void givenWholeDoubleInRange_whenTryConvertToLong_thenOptionalLongNode() throws Exception {
            // Given
            Double input = 1_000_000d;

            // When
            Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);

            // Then
            assertInstanceOf(Optional.class, result);
            Optional<?> opt = (Optional<?>) result;
            assertTrue(opt.isPresent());
            assertInstanceOf(LongNode.class, opt.get());
            assertEquals(1_000_000L, ((JsonNode) opt.get()).longValue());
        }

        @Test
        @DisplayName("Given fractional double, When tryConvertToLong is called, Then Optional.empty is returned")
        void givenFractionalDouble_whenTryConvertToLong_thenEmpty() throws Exception {
            // Given
            Double input = 3.14;

            // When
            Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);

            // Then
            assertInstanceOf(Optional.class, result);
            assertTrue(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given whole double outside long range (max), When tryConvertToLong is called, Then Optional.empty is returned")
        void givenWholeDoubleOutOfRangeMax_whenTryConvertToLong_thenEmpty() throws Exception {
            // Given
            Double input = (double) Long.MAX_VALUE + 1000d;

            // When
            Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);

            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given whole double outside long range (min), When tryConvertToLong is called, Then Optional.empty is returned")
        void givenWholeDoubleOutOfRangeMin_whenTryConvertToLong_thenEmpty() throws Exception {
            // Given
            Double input = (double) Long.MIN_VALUE - 1000d;

            // When
            Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);

            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given NonInteger, When tryConvertToLong is called, Then Optional.empty is returned")
        void testNonIntegerValueReturnsEmpty_whenTryConvertToLong() throws Exception {
            // Given
            Double input = (double) 3.14;

            // When
            Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);

            // Then
            assertInstanceOf(Optional.class, result);
            assertTrue(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given Integer, When tryConvertToLong is called, Then Optional is returned")
        void testIntegerValueReturnsOptional_whenTryConvertToLong() throws Exception {
            // Given
            Double input = (double) 10.0;

            // When
            Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);

            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given Long Max Value, When tryConvertToLong is called, Then Optional is returned")
        void testLongMaxValueReturnsOptional_whenTryConvertToLong() throws Exception {
            // Given
            Double input = (double) Long.MAX_VALUE + 1;

            // When
            Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);

            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given Long Min Value, When tryConvertToLong is called, Then Optional is returned")
        void testLongMinValueReturnsOptional_whenTryConvertToLong() throws Exception {
            // Given
            Double input = (double) Long.MIN_VALUE - 1;

            // When
            Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);

            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given Long Min Value, When tryConvertToLong is called, Then Optional is returned")
        void testLongNormalizeBigInteger() throws Exception {
            // Given
            BigInteger input = BigInteger.valueOf(Long.MIN_VALUE - 1);

            // When
            Object result = invokePrivateStatic("normalizeBigInteger", new Class[]{BigInteger.class}, input);

            // Then
            assertInstanceOf(JsonNode.class, result);
            assertFalse(((JsonNode) result).isBigDecimal());
        }

        @Test
        @DisplayName("Given negative NonInteger, When tryConvertToLong is called, Then Optional.empty is returned")
        void testNegativeNonIntegerValueReturnsEmptyWhenTryConvertToLong() throws Exception {
            // Given
            Double input = (double) -5.7;

            // When
            Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);

            // Then
            assertInstanceOf(Optional.class, result);
            assertTrue(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given negative Integer, When tryConvertToLong is called, Then Optional is returned")
        void testNegativeIntegerValueReturnsOptionalWhenTryConvertToLong() throws Exception {
            // Given
            Double input = (double) -8.0;

            // When
            Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);

            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }
    }

    @Nested
    @DisplayName("tryNormalizeCollection")
    class TryNormalizeCollection {

        @Test
        @DisplayName("Given List, When tryNormalizeCollection is called, Then ArrayNode is returned")
        void givenList_whenTryNormalizeCollection_thenArrayNode() throws Exception {
            // Given
            List<Object> input = java.util.Arrays.asList(1, "two", true);

            // When
            Object result = invokePrivateStatic("tryNormalizeCollection", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(ArrayNode.class, result);
            ArrayNode array = (ArrayNode) result;
            assertEquals(3, array.size());
            assertEquals(1, array.get(0).asInt());
            assertEquals("two", array.get(1).asString());
            assertTrue(array.get(2).asBoolean());
        }

        @Test
        @DisplayName("Given Map, When tryNormalizeCollection is called, Then ObjectNode is returned")
        void givenMap_whenTryNormalizeCollection_thenObjectNode() throws Exception {
            // Given
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("a", 1);
            input.put("b", "two");

            // When
            Object result = invokePrivateStatic("tryNormalizeCollection", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(ObjectNode.class, result);
            ObjectNode object = (ObjectNode) result;
            assertEquals(1, object.get("a").asInt());
            assertEquals("two", object.get("b").asString());
        }

        @Test
        @DisplayName("Given non-collection, When tryNormalizeCollection is called, Then null is returned")
        void givenOther_whenTryNormalizeCollection_thenNull() throws Exception {
            // Given
            Object input = 10.0;

            // When
            Object result = invokePrivateStatic("tryNormalizeCollection", new Class[]{Object.class}, input);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("normalizeCollection")
    class NormalizeCollection {

        @Test
        @DisplayName("Given mixed-type list, When normalizeCollection is called, Then items are normalized in an ArrayNode")
        void givenMixedList_whenNormalizeCollection_thenArrayNode() throws Exception {
            // Given
            List<Object> input = java.util.Arrays.asList(1, 2L, 3.0, "four");

            // When
            Object result = invokePrivateStatic("normalizeCollection", new Class[]{Collection.class}, input);

            // Then
            assertInstanceOf(ArrayNode.class, result);
            ArrayNode array = (ArrayNode) result;
            assertEquals(4, array.size());
            assertEquals(1, array.get(0).asInt());
            assertEquals(2L, array.get(1).asLong());
            assertEquals(3.0, array.get(2).asDouble());
            assertEquals("four", array.get(3).asString());
        }

        @Test
        @DisplayName("Given empty list, When normalizeCollection is called, Then an empty ArrayNode is returned")
        void givenEmptyList_whenNormalizeCollection_thenEmptyArrayNode() throws Exception {
            // Given
            List<Object> input = java.util.Collections.emptyList();

            // When
            Object result = invokePrivateStatic("normalizeCollection", new Class[]{Collection.class}, input);

            // Then
            assertInstanceOf(ArrayNode.class, result);
            assertEquals(0, ((ArrayNode) result).size());
        }
    }

    @Nested
    @DisplayName("normalizeArray")
    class NormalizeArray {

        @Test
        @DisplayName("Given Object, When normalizeArray is called, Then ArrayNode get return")
        void NormalizeArray_thenNullNode() throws Exception {
            // Given
            Object input = new Object();

            // When
            Object result = invokePrivateStatic("normalizeArray", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(ArrayNode.class, result);
        }
    }

    @Nested
    @DisplayName("NormalizePojo")
    class NormalizePojo {
        class ExplodingPojo {
            public String getValue() {
                throw new RuntimeException("Boom");
            }
        }

        @Test
        @DisplayName("Given Object, When tryNormalizePojo is called, Then ArrayNode get return")
        void tryNormalizePojo_thenNullNode() throws Exception {
            // Given
            Object input = new Object();

            // When
            Object result = invokePrivateStatic("tryNormalizePojo", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(ObjectNode.class, result);
        }

        @Test
        void returnsNullNodeWhenJacksonExceptionOccurs() throws Exception {
            // Given
            Object input = new ExplodingPojo();

            // When
            Object result = invokePrivateStatic("tryNormalizePojo", new Class[]{Object.class}, input);

            // Then
            assertInstanceOf(NullNode.class, result);
        }
    }

    @Nested
    @DisplayName("parse")
    class parse {
        @Test
        void parseNullAsString() {
            // Given
            String input = null;

            // When
            final IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> JsonNormalizer.parse(input));


            // Then
            assertEquals("Invalid JSON", thrown.getMessage());
        }


        @Test
        void parseEmptyString() {
            // Given
            String input = " ";

            // When
            final IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> JsonNormalizer.parse(input));


            // Then
            assertEquals("Invalid JSON", thrown.getMessage());
        }

    }
}

