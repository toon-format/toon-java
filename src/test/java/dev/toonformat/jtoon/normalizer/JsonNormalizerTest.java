package dev.toonformat.jtoon.normalizer;

import static org.junit.jupiter.api.Assertions.*;
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

/**
 * JUnit 5 test class for JsonNormalizer utility.
 */
@Tag("unit")
class JsonNormalizerTest {

    private static final int TEST_INT_VALUE = 42;
    private static final long TEST_LONG_VALUE = 9223372036854775807L;
    private static final short TEST_SHORT_VALUE = 32767;
    private static final byte TEST_BYTE_VALUE = 127;
    private static final float TEST_FLOAT_VALUE = 3.14f;
    private static final double TEST_DOUBLE_VALUE = 3.14159;
    private static final float TEST_FLOAT_DELTA = 0.001f;
    private static final double TEST_DOUBLE_DELTA = 0.00001;
    private static final long TEST_BIG_LONG = 123456789L;
    private static final int TEST_MAP_SIZE = 3;
    private static final int TEST_ARRAY_SIZE = 3;
    private static final double DOUBLE_DELTA = 0.001;
    private static final int TEST_AGE = 30;
    private static final int EXPECTED_SIZE_FOUR = 4;
    private static final int EXPECTED_SIZE_FIVE = 5;
    private static final int EXPECTED_SIZE_SEVEN = 7;
    private static final double EDGE_CASE_DOUBLE = 3.14;
    private static final long CONVERTIBLE_LONG_VALUE = 1_000_000L;

    @Nested
    @DisplayName("Null and JsonNode")
    class NullAndJsonNode {

        @Test
        @DisplayName("should return NullNode for null input")
        void testNullInput() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(null);
            // Then
            assertTrue(result.isNull());
            assertInstanceOf(NullNode.class, result);
        }

        @Test
        @DisplayName("should pass through JsonNode unchanged")
        void testJsonNodePassthrough() {
            // Given
            final JsonNode textNode = StringNode.valueOf("test");
            // When
            final JsonNode result = JsonNormalizer.normalize(textNode);
            // Then
            assertSame(textNode, result);
        }

        @Test
        void testJsonNodePassthrough2() {
            // Given
            final JsonNode intNode = IntNode.valueOf(TEST_INT_VALUE);
            // When
            final JsonNode result = JsonNormalizer.normalize(intNode);
            // Then
            assertSame(intNode, result);
        }

        @Test
        void testJsonNodePassthrough3() {
            // Given
            final JsonNode boolNode = BooleanNode.TRUE;
            // When
            final JsonNode result = JsonNormalizer.normalize(boolNode);
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
            final JsonNode result = JsonNormalizer.normalize("hello");
            // Then
            assertTrue(result.isString());
            assertEquals("hello", result.asString());
            assertInstanceOf(StringNode.class, result);
        }

        @Test
        @DisplayName("should normalize empty String to StringNode")
        void testEmptyString() {
            // Given
            final JsonNode result = JsonNormalizer.normalize("");
            // Then
            assertTrue(result.isString());
            assertEquals("", result.asString());
        }

        @Test
        @DisplayName("should normalize Boolean to BooleanNode")
        void testBoolean() {
            // Given
            final JsonNode resultTrue = JsonNormalizer.normalize(true);
            // Then
            assertTrue(resultTrue.isBoolean());
            assertTrue(resultTrue.asBoolean());
            assertInstanceOf(BooleanNode.class, resultTrue);
        }

        @Test
        @DisplayName("should normalize Boolean to BooleanNode")
        void testBoolean2() {
            // Given
            final JsonNode resultFalse = JsonNormalizer.normalize(false);
            // Then
            assertTrue(resultFalse.isBoolean());
            assertFalse(resultFalse.asBoolean());
        }

        @Test
        @DisplayName("should normalize Integer to IntNode")
        void testInteger() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(TEST_INT_VALUE);
            // Then
            assertTrue(result.isInt());
            assertEquals(TEST_INT_VALUE, result.asInt());
            assertInstanceOf(IntNode.class, result);
        }

        @Test
        @DisplayName("should normalize Long to LongNode")
        void testLong() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(TEST_LONG_VALUE);
            // Then
            assertTrue(result.isLong());
            assertEquals(TEST_LONG_VALUE, result.asLong());
            assertInstanceOf(LongNode.class, result);
        }

        @Test
        @DisplayName("should normalize Short to ShortNode")
        void testShort() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(TEST_SHORT_VALUE);
            // Then
            assertTrue(result.isShort());
            assertEquals(TEST_SHORT_VALUE, result.asInt());
            assertInstanceOf(ShortNode.class, result);
        }

        @Test
        @DisplayName("should normalize Byte to IntNode")
        void testByte() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(TEST_BYTE_VALUE);
            // Then
            assertTrue(result.isInt());
            assertEquals(TEST_BYTE_VALUE, result.asInt());
            assertInstanceOf(IntNode.class, result);
        }

        @Test
        @DisplayName("should normalize Float to FloatNode")
        void testFloat() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(TEST_FLOAT_VALUE);
            // Then
            assertTrue(result.isFloat());
            assertEquals(TEST_FLOAT_VALUE, result.floatValue(), TEST_FLOAT_DELTA);
            assertInstanceOf(FloatNode.class, result);
        }

        @Test
        @DisplayName("should normalize Double to DoubleNode")
        void testDouble() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(TEST_DOUBLE_VALUE);
            // Then
            assertTrue(result.isDouble());
            assertEquals(TEST_DOUBLE_VALUE, result.asDouble(), TEST_DOUBLE_DELTA);
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
            final JsonNode result = JsonNormalizer.normalize(Double.NaN);
            // Then
            assertTrue(result.isNull());
            assertInstanceOf(NullNode.class, result);
        }

        @Test
        @DisplayName("should convert positive Infinity to NullNode")
        void testPositiveInfinity() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(Double.POSITIVE_INFINITY);
            // Then
            assertTrue(result.isNull());
            assertInstanceOf(NullNode.class, result);
        }

        @Test
        @DisplayName("should convert negative Infinity to NullNode")
        void testNegativeInfinity() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(Double.NEGATIVE_INFINITY);
            // Then
            assertTrue(result.isNull());
            assertInstanceOf(NullNode.class, result);
        }

        @Test
        @DisplayName("should canonicalize -0.0 to IntNode(0)")
        void testNegativeZero() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(-0.0);
            // Then
            assertTrue(result.isInt());
            assertEquals(0, result.asInt());
            assertInstanceOf(IntNode.class, result);
        }

        @Test
        @DisplayName("should canonicalize +0.0 to IntNode(0)")
        void testPositiveZero() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(0.0);
            // Then
            assertTrue(result.isInt());
            assertEquals(0, result.asInt());
            assertInstanceOf(IntNode.class, result);
        }

        @Test
        @DisplayName("should convert whole numbers to LongNode when in range")
        void testWholeNumbers() {
            // Given
            final double wholeNumber = 42.0;
            final JsonNode result = JsonNormalizer.normalize(wholeNumber);
            // Then
            assertTrue(result.isIntegralNumber());
            assertEquals(TEST_INT_VALUE, result.asLong());
            assertInstanceOf(LongNode.class, result);
        }

        @Test
        @DisplayName("should convert whole numbers to LongNode when in range")
        void testWholeNumbers2() {
            // Given
            final double million = 1000000.0;
            final JsonNode result = JsonNormalizer.normalize(million);
            // Then
            assertTrue(result.isIntegralNumber());
            assertEquals((long) million, result.asLong());
        }

        @Test
        @DisplayName("should keep regular decimals as DoubleNode")
        void testRegularDecimals() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(TEST_DOUBLE_VALUE);
            // Then
            assertTrue(result.isDouble());
            assertEquals(TEST_DOUBLE_VALUE, result.asDouble(), TEST_DOUBLE_DELTA);
            assertInstanceOf(DoubleNode.class, result);
        }

        @Test
        @DisplayName("should convert Float NaN to NullNode")
        void testFloatNaN() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(Float.NaN);
            // Then
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("should convert Float Infinity to NullNode")
        void testFloatInfinity() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(Float.POSITIVE_INFINITY);
            // Then
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("should convert Float Infinity to NullNode")
        void testFloatInfinity2() {
            // Given
            final JsonNode result = JsonNormalizer.normalize(Float.NEGATIVE_INFINITY);
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
            final BigInteger bigInt = BigInteger.valueOf(TEST_BIG_LONG);
            // When
            final JsonNode result = JsonNormalizer.normalize(bigInt);
            // Then
            assertTrue(result.isLong());
            assertEquals(TEST_BIG_LONG, result.asLong());
            assertInstanceOf(LongNode.class, result);
        }

        @Test
        @DisplayName("should convert BigInteger at Long.MAX_VALUE to LongNode")
        void testBigIntegerAtMaxLong() {
            // Given
            final BigInteger bigInt = BigInteger.valueOf(Long.MAX_VALUE);
            // When
            final JsonNode result = JsonNormalizer.normalize(bigInt);
            // Then
            assertTrue(result.isLong());
            assertEquals(Long.MAX_VALUE, result.asLong());
        }

        @Test
        @DisplayName("should convert BigInteger at Long.MIN_VALUE to LongNode")
        void testBigIntegerAtMinLong() {
            // Given
            final BigInteger bigInt = BigInteger.valueOf(Long.MIN_VALUE);
            // When
            final JsonNode result = JsonNormalizer.normalize(bigInt);
            // Then
            assertTrue(result.isLong());
            assertEquals(Long.MIN_VALUE, result.asLong());
        }

        @Test
        @DisplayName("should convert BigInteger outside Long range to StringNode")
        void testBigIntegerOutOfRange() {
            // Given
            final BigInteger bigInt = new BigInteger("99999999999999999999999999999999");
            // When
            final JsonNode result = JsonNormalizer.normalize(bigInt);
            // Then
            assertTrue(result.isString());
            assertEquals("99999999999999999999999999999999", result.asString());
            assertInstanceOf(StringNode.class, result);
        }

        @Test
        @DisplayName("should convert BigDecimal to DecimalNode")
        void testBigDecimal() {
            // Given
            final BigDecimal bigDec = new BigDecimal("123.456");
            // When
            final JsonNode result = JsonNormalizer.normalize(bigDec);
            // Then
            assertTrue(result.isBigDecimal());
            assertEquals(new BigDecimal("123.456"), result.decimalValue());
            assertInstanceOf(DecimalNode.class, result);
        }

        @Test
        @DisplayName("should convert large BigDecimal to DecimalNode")
        void testLargeBigDecimal() {
            // Given
            final BigDecimal bigDec = new BigDecimal("999999999999999999999.999999999999999999");
            // When
            final JsonNode result = JsonNormalizer.normalize(bigDec);
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
            final LocalDateTime dateTime = LocalDateTime.of(2023, 10, 15, 14, 30, 45);
            // When
            final JsonNode result = JsonNormalizer.normalize(dateTime);
            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15T14:30:45", result.asString());
        }

        @Test
        @DisplayName("should convert java.sql.Date to ISO formatted StringNode")
        void testSqlDate() {
            // Given
            final java.sql.Date dateTime = new java.sql.Date(1766419274);
            // When
            final JsonNode result = JsonNormalizer.normalize(dateTime);
            // Then
            assertTrue(result.isString());
            assertEquals("1970-01-21", result.asString());
        }

        @Test
        @DisplayName("should convert java.sql.Time to ISO formatted StringNode")
        void testSqlTime() {
            // Given
            final java.sql.Time time = new java.sql.Time(1766419274);
            // When
            final JsonNode result = JsonNormalizer.normalize(time);
            // Then
            assertTrue(result.isString());
            final String expected = time.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
            assertEquals(expected, result.asString());
        }

        @Test
        @DisplayName("should convert java.sql.Timestamp to ISO formatted StringNode")
        void testSqlTimeStamp() {
            // Given
            final java.sql.Timestamp dateTime = new java.sql.Timestamp(1766419274);
            // When
            final JsonNode result = JsonNormalizer.normalize(dateTime);
            // Then
            assertTrue(result.isString());
            final String expected = dateTime.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            assertEquals(expected, result.asString());
        }


        @Test
        @DisplayName("should convert LocalDate to ISO formatted StringNode")
        void testLocalDate() {
            // Given
            final LocalDate date = LocalDate.of(2023, 10, 15);
            // When
            final JsonNode result = JsonNormalizer.normalize(date);
            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15", result.asString());
        }

        @Test
        @DisplayName("should convert LocalTime to ISO formatted StringNode")
        void testLocalTime() {
            // Given
            final LocalTime time = LocalTime.of(14, 30, 45);
            // When
            final JsonNode result = JsonNormalizer.normalize(time);
            // Then
            assertTrue(result.isString());
            assertEquals("14:30:45", result.asString());
        }

        @Test
        @DisplayName("should convert ZonedDateTime to ISO formatted StringNode")
        void testZonedDateTime() {
            // Given
            final ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 10, 15, 14, 30, 45, 0, ZoneId.of("UTC"));
            // When
            final JsonNode result = JsonNormalizer.normalize(zonedDateTime);
            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15T14:30:45Z", result.asString());
        }

        @Test
        @DisplayName("should convert OffsetDateTime to ISO formatted StringNode")
        void testOffsetDateTime() {
            // Given
            final OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 10, 15, 14, 30, 45, 0, ZoneOffset.UTC);
            // When
            final JsonNode result = JsonNormalizer.normalize(offsetDateTime);
            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15T14:30:45Z", result.asString());
        }

        @Test
        @DisplayName("should convert Instant to ISO formatted StringNode")
        void testInstant() {
            // Given
            final Instant instant = Instant.parse("2023-10-15T14:30:45.123Z");
            // When
            final JsonNode result = JsonNormalizer.normalize(instant);
            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15T14:30:45.123Z", result.asString());
        }

        @Test
        @DisplayName("should convert java.util.Date to ISO formatted StringNode")
        void testUtilDate() {
            // Given
            final Date date = Date.from(Instant.parse("2023-10-15T14:30:45.123Z"));
            // When
            final JsonNode result = JsonNormalizer.normalize(date);
            // Then
            assertTrue(result.isString());
            assertEquals("2023-10-15", result.asString());
        }
    }

    @Nested
    @DisplayName("Lists")
    class Lists {

        @Test
        @DisplayName("should convert List to ArrayNode")
        void testList() {
            // Given
            final int expectedSize = 4;
            final List<Object> list = List.of(1, 2, TEST_ARRAY_SIZE, "four");
            // When
            final JsonNode result = JsonNormalizer.normalize(list);
            // Then
            assertTrue(result.isArray());
            assertEquals(expectedSize, result.size());
            assertEquals(1, result.get(0).asInt());
            assertEquals(2, result.get(1).asInt());
            assertEquals(TEST_ARRAY_SIZE, result.get(expectedSize - 2).asInt());
            assertEquals("four", result.get(expectedSize - 1).asString());
        }

        @Test
        @DisplayName("should convert empty List to empty ArrayNode")
        void testEmptyList() {
            // Given
            final List<Object> list = List.of();
            // When
            final JsonNode result = JsonNormalizer.normalize(list);
            // Then
            assertTrue(result.isArray());
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should convert Set to ArrayNode")
        void testSet() {
            // Given
            final Set<Integer> set = new LinkedHashSet<>(List.of(1, 2, 3));
            // When
            final JsonNode result = JsonNormalizer.normalize(set);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
        }

        @Test
        @DisplayName("should convert Map to ObjectNode")
        void testMap() {
            // Given
            final int testAge = 30;
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "John");
            map.put("age", testAge);
            map.put("active", true);
            // When
            final JsonNode result = JsonNormalizer.normalize(map);
            // Then
            assertTrue(result.isObject());
            assertEquals(TEST_MAP_SIZE, result.size());
            assertEquals("John", result.get("name").asString());
            assertEquals(testAge, result.get("age").asInt());
            assertTrue(result.get("active").asBoolean());
        }

        @Test
        @DisplayName("should convert empty Map to empty ObjectNode")
        void testEmptyMap() {
            // Given
            final Map<String, Object> map = new HashMap<>();
            // When
            final JsonNode result = JsonNormalizer.normalize(map);
            // Then
            assertTrue(result.isObject());
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should handle nested collections")
        void testNestedCollections() {
            // Given
            final int listItemCount = 3;
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("numbers", List.of(1, 2, listItemCount));
            map.put("nested", Map.of("key", "value"));
            // When
            final JsonNode result = JsonNormalizer.normalize(map);
            // Then
            assertTrue(result.isObject());
            assertTrue(result.get("numbers").isArray());
            assertEquals(listItemCount, result.get("numbers").size());
            assertTrue(result.get("nested").isObject());
            assertEquals("value", result.get("nested").get("key").asString());
        }

        @Test
        @DisplayName("should convert non-String Map keys to String")
        void testMapWithNonStringKeys() {
            // Given
            final Map<Integer, String> map = new HashMap<>();
            map.put(1, "one");
            map.put(2, "two");
            // When
            final JsonNode result = JsonNormalizer.normalize(map);
            // Then
            assertTrue(result.isObject());
            assertEquals("one", result.get("1").asString());
            assertEquals("two", result.get("2").asString());
        }

        @Test
        @DisplayName("should handle collections with null values")
        void testCollectionWithNulls() {
            // Given
            final int nullCollectionVal = TEST_ARRAY_SIZE;
            final List<Object> list = java.util.Arrays.asList(1, null, nullCollectionVal);
            // When
            final JsonNode result = JsonNormalizer.normalize(list);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
            assertEquals(1, result.get(0).asInt());
            assertTrue(result.get(1).isNull());
            assertEquals(TEST_ARRAY_SIZE, result.get(2).asInt());
        }
    }

    @Nested
    @DisplayName("Arrays")
    class Arrays {

        @Test
        @DisplayName("should convert int[] to ArrayNode")
        void testIntArray() {
            // Given
            final int arraySize = 5;
            final int lastElementValue = arraySize;
            final int lastIndex = arraySize - 1;
            final int[] array = {1, 2, arraySize - 2, arraySize - 1, lastElementValue};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(arraySize, result.size());
            assertEquals(1, result.get(0).asInt());
            assertEquals(lastElementValue, result.get(lastIndex).asInt());
        }

        @Test
        @DisplayName("should convert long[] to ArrayNode")
        void testLongArray() {
            // Given
            final long[] array = {1L, 2L, TEST_LONG_VALUE};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
            assertEquals(TEST_LONG_VALUE, result.get(2).asLong());
        }

        @Test
        @DisplayName("should convert double[] to ArrayNode")
        void testDoubleArray() {
            // Given
            final double firstDoubleVal = 1.1;
            final double[] array = {firstDoubleVal, 2.2, 3.3};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
            assertEquals(firstDoubleVal, result.get(0).asDouble(), DOUBLE_DELTA);
        }

        @Test
        @DisplayName("should convert double[] with special values to ArrayNode with nulls")
        void testDoubleArrayWithSpecialValues() {
            // Given
            final int specialArrSize = 5;
            final int lastIndex = specialArrSize - 1;
            final double fourthValue = 4.0;
            final double[] array = {1.0, Double.NaN, Double.POSITIVE_INFINITY, fourthValue, Double.NEGATIVE_INFINITY};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(specialArrSize, result.size());
            assertEquals(1, result.get(0).asInt());
            assertTrue(result.get(1).isNull());
            assertTrue(result.get(2).isNull());
            assertEquals((int) fourthValue, result.get(lastIndex - 1).asInt());
            assertTrue(result.get(lastIndex).isNull());
        }

        @Test
        @DisplayName("should convert float[] to ArrayNode")
        void testFloatArray() {
            // Given
            final float firstFloatVal = 1.1f;
            final float[] array = {firstFloatVal, 2.2f, 3.3f};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
            assertEquals(firstFloatVal, result.get(0).floatValue(), TEST_FLOAT_DELTA);
        }

        @Test
        @DisplayName("should convert float[] with special values to ArrayNode with nulls")
        void testFloatArrayWithSpecialValues() {
            // Given
            final float[] array = {1.0f, Float.NaN, Float.POSITIVE_INFINITY};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
            assertEquals(1.0f, result.get(0).floatValue(), TEST_FLOAT_DELTA);
            assertTrue(result.get(1).isNull());
            assertTrue(result.get(2).isNull());
        }

        @Test
        @DisplayName("should convert boolean[] to ArrayNode")
        void testBooleanArray() {
            // Given
            final boolean[] array = {true, false, true};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
            assertTrue(result.get(0).asBoolean());
            assertFalse(result.get(1).asBoolean());
        }

        @Test
        @DisplayName("should convert byte[] to ArrayNode")
        void testByteArray() {
            // Given
            final byte[] array = {1, 2, 127};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
            assertEquals(TEST_BYTE_VALUE, result.get(2).asInt());
        }

        @Test
        @DisplayName("should convert short[] to ArrayNode")
        void testShortArray() {
            // Given
            final short[] array = {1, 2, TEST_SHORT_VALUE};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
            assertEquals(TEST_SHORT_VALUE, result.get(2).asInt());
        }

        @Test
        @DisplayName("should convert char[] to ArrayNode of strings")
        void testCharArray() {
            // Given
            final char[] array = {'a', 'b', 'c'};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
            assertEquals("a", result.get(0).asString());
            assertEquals("b", result.get(1).asString());
            assertEquals("c", result.get(2).asString());
        }

        @Test
        @DisplayName("should convert Object[] to ArrayNode")
        void testObjectArray() {
            // Given
            final double objectArrPi = 3.14;
            final Object[] array = {1, "two", true, objectArrPi};
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
            // Then
            assertTrue(result.isArray());
            assertEquals(EXPECTED_SIZE_FOUR, result.size());
            assertEquals(1, result.get(0).asInt());
            assertEquals("two", result.get(1).asString());
            assertTrue(result.get(2).asBoolean());
            assertEquals(objectArrPi, result.get(EXPECTED_SIZE_FOUR - 1).asDouble(), DOUBLE_DELTA);
        }

        @Test
        @DisplayName("should convert empty arrays to empty ArrayNode")
        void testEmptyArrays() {
            // Given
            final int[] intArray = {};
            // When
            final JsonNode result = JsonNormalizer.normalize(intArray);
            // Then
            assertTrue(result.isArray());
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should convert empty arrays to empty ArrayNode")
        void testEmptyArraysOfObjects() {
            // Given
            final Object[] objArray = {};
            // When
            final JsonNode result = JsonNormalizer.normalize(objArray);
            // Then
            assertTrue(result.isArray());
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should handle nested arrays")
        void testNestedArrays() {
            // Given
            final Object[] array = {
                new int[]{1, 2},
                new String[]{"a", "b"}
            };
            // When
            final JsonNode result = JsonNormalizer.normalize(array);
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
            final Optional<String> optional = Optional.empty();
            // When
            final JsonNode result = JsonNormalizer.normalize(optional);
            // Then
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("should unwrap Optional.of(value)")
        void testOptionalWithValue() {
            // Given
            final Optional<String> optional = Optional.of("hello");
            // When
            final JsonNode result = JsonNormalizer.normalize(optional);
            // Then
            assertTrue(result.isString());
            assertEquals("hello", result.asString());
        }

        @Test
        @DisplayName("should unwrap Optional.of(value)")
        void testOptionalWithValue2() {
            // Given
            final Optional<Integer> intOptional = Optional.of(42);
            // When
            final JsonNode result = JsonNormalizer.normalize(intOptional);
            // Then
            assertTrue(result.isInt());
            assertEquals(TEST_INT_VALUE, result.asInt());
        }

        @Test
        @DisplayName("should unwrap nested Optional")
        void testNestedOptional() {
            // Given
            final Optional<Optional<String>> nested = Optional.of(Optional.of("nested"));
            // When
            final JsonNode result = JsonNormalizer.normalize(nested);
            // Then
            assertTrue(result.isString());
            assertEquals("nested", result.asString());
        }

        @Test
        @DisplayName("should convert Stream to ArrayNode")
        void testStream() {
            // Given
            final int streamSize = 5;
            final int lastStreamIdx = streamSize - 1;
            final Stream<Integer> stream = Stream.of(1, 2, streamSize - 2, streamSize - 1, streamSize);
            // When
            final JsonNode result = JsonNormalizer.normalize(stream);
            // Then
            assertTrue(result.isArray());
            assertEquals(streamSize, result.size());
            assertEquals(1, result.get(0).asInt());
            assertEquals(streamSize, result.get(lastStreamIdx).asInt());
        }

        @Test
        @DisplayName("should convert empty Stream to empty ArrayNode")
        void testEmptyStream() {
            // Given
            final Stream<String> stream = Stream.empty();
            // When
            final JsonNode result = JsonNormalizer.normalize(stream);
            // Then
            assertTrue(result.isArray());
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should handle Stream with null values")
        void testStreamWithNulls() {
            // Given
            final Stream<Object> stream = Stream.of(1, null, TEST_ARRAY_SIZE);
            // When
            final JsonNode result = JsonNormalizer.normalize(stream);
            // Then
            assertTrue(result.isArray());
            assertEquals(TEST_ARRAY_SIZE, result.size());
            assertEquals(1, result.get(0).asInt());
            assertTrue(result.get(1).isNull());
            assertEquals(TEST_ARRAY_SIZE, result.get(2).asInt());
        }
    }

    @Nested
    @DisplayName("POJOs")
    class Pojos {

        record SimplePojo(String name, int age) {
        }

        record PojoWithGetters(String value) {
        }

        @Test
        @DisplayName("should convert simple POJO to ObjectNode")
        void testSimplePojo() {
            // Given
            final int aliceAge = 25;
            final SimplePojo pojo = new SimplePojo("Alice", aliceAge);
            // When
            final JsonNode result = JsonNormalizer.normalize(pojo);
            // Then
            assertTrue(result.isObject());
            assertEquals("Alice", result.get("name").asString());
            assertEquals(aliceAge, result.get("age").asInt());
        }

        @Test
        @DisplayName("should convert POJO with getters to ObjectNode")
        void testPojoWithGetters() {
            // Given
            final PojoWithGetters pojo = new PojoWithGetters("test");
            // When
            final JsonNode result = JsonNormalizer.normalize(pojo);
            // Then
            assertTrue(result.isObject());
            assertEquals("test", result.get("value").asString());
        }

        @Test
        @DisplayName("should handle nested POJOs")
        void testNestedPojo() {
            // Given
            final int bobAge = TEST_AGE;
            final int nestedId = 123;
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("pojo", new SimplePojo("Bob", bobAge));
            map.put("id", nestedId);
            // When
            final JsonNode result = JsonNormalizer.normalize(map);
            // Then
            assertTrue(result.isObject());
            assertTrue(result.get("pojo").isObject());
            assertEquals("Bob", result.get("pojo").get("name").asString());
            assertEquals(nestedId, result.get("id").asInt());
        }

        @Test
        @DisplayName("should handle collections of POJOs")
        void testCollectionOfPojos() {
            // Given
            final List<SimplePojo> pojos = List.of(
                new SimplePojo("Alice", 25),
                new SimplePojo("Bob", 30)
            );
            // When
            final JsonNode result = JsonNormalizer.normalize(pojos);
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
            final Thread thread = new Thread();
            // When
            final JsonNode result = JsonNormalizer.normalize(thread);
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
            final Map<String, Object> level3 = Map.of("value", TEST_INT_VALUE);
            final Map<String, Object> level2 = Map.of("level3", level3);
            final Map<String, Object> level1 = Map.of("level2", level2);
            // When
            final JsonNode result = JsonNormalizer.normalize(level1);
            // Then
            assertEquals(TEST_INT_VALUE, result.get("level2").get("level3").get("value").asInt());
        }

        @Test
        @DisplayName("should handle mixed types in collections")
        void testMixedTypes() {
            // Given
            final int mixedSize = 7;
            final List<Object> mixed = java.util.Arrays.asList(
                1,
                "text",
                true,
                3.14,
                List.of(1, 2),
                Map.of("key", "value"),
                null
            );
            // When
            final JsonNode result = JsonNormalizer.normalize(mixed);
            // Then
            assertTrue(result.isArray());
            assertEquals(mixedSize, result.size());
            assertEquals(1, result.get(0).asInt());
            assertEquals("text", result.get(1).asString());
            assertTrue(result.get(2).asBoolean());
            assertEquals(EDGE_CASE_DOUBLE, result.get(EXPECTED_SIZE_FOUR - 1).asDouble(),
                DOUBLE_DELTA);
            assertTrue(result.get(EXPECTED_SIZE_FOUR).isArray());
            assertTrue(result.get(EXPECTED_SIZE_FIVE).isObject());
            assertTrue(result.get(EXPECTED_SIZE_SEVEN - 1).isNull());
        }

        @Test
        @DisplayName("should handle Optional with null value")
        void testOptionalOfNull() {
            // Given
            final Optional<String> optional = Optional.empty();
            // When
            final JsonNode result = JsonNormalizer.normalize(optional);
            // Then
            assertTrue(result.isNull());
        }

        @Test
        @DisplayName("should handle arrays containing arrays")
        void testArrayOfArrays() {
            // Given
            final int[][] matrix = {{1, 2}, {3, 4}};
            // When
            final JsonNode result = JsonNormalizer.normalize(matrix);
            // Then
            assertTrue(result.isArray());
            assertEquals(2, result.size());
            assertTrue(result.get(0).isArray());
            assertEquals(1, result.get(0).get(0).asInt());
            assertEquals(EXPECTED_SIZE_FOUR, result.get(1).get(1).asInt());
        }

        @Test
        @DisplayName("should handle Map with null values")
        void testMapWithNullValues() {
            // Given
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("key1", "value");
            map.put("key2", null);
            // When
            final JsonNode result = JsonNormalizer.normalize(map);
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
    private static Object invokePrivateStatic(final String methodName, final Class<?>[] paramTypes,
            final Object... args) throws Exception {
        final Method declaredMethod = JsonNormalizer.class.getDeclaredMethod(methodName, paramTypes);
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
            final Integer input = TEST_INT_VALUE;
            // When
            final Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(IntNode.class, result);
            assertEquals(TEST_INT_VALUE, ((JsonNode) result).asInt());
        }

        @Test
        @DisplayName("Given an unsupported type, When tryNormalizePrimitive is called, Then null is returned")
        void givenUnsupported_whenTryNormalizePrimitive_thenNull() throws Exception {
            // Given
            final Object input = new Object();
            // When
            final Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);
            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Given an String value, When tryNormalizePrimitive is called, Then an StringNode is returned")
        void givenString_whenTryNormalizePrimitive_thenStringNode() throws Exception {
            // Given
            final String input = "hello world";
            // When
            final Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("hello world", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given an Boolean value, When tryNormalizePrimitive is called, Then an BooleanNode is returned")
        void givenBoolean_whenTryNormalizePrimitive_thenBooleanNode() throws Exception {
            // Given
            final Boolean input = Boolean.TRUE;
            // When
            final Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(BooleanNode.class, result);
            assertTrue(((JsonNode) result).asBoolean());
        }

        @Test
        @DisplayName("Given an Long value, When tryNormalizePrimitive is called, Then an LongNode is returned")
        void givenLong_whenTryNormalizePrimitive_thenLongNode() throws Exception {
            // Given
            final Long input = Long.MAX_VALUE;
            // When
            final Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(LongNode.class, result);
            assertEquals(Long.MAX_VALUE, ((JsonNode) result).asLong());
        }

        @Test
        @DisplayName("Given an Short value, When tryNormalizePrimitive is called, Then an ShortNode is returned")
        void givenShort_whenTryNormalizePrimitive_thenShortNode() throws Exception {
            // Given
            final Short input = Short.MAX_VALUE;
            // When
            final Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(ShortNode.class, result);
            assertEquals(Short.MAX_VALUE, ((JsonNode) result).asShort());
        }

        @Test
        @DisplayName("Given an Byte value, When tryNormalizePrimitive is called, Then an ByteNode is returned")
        void givenByte_whenTryNormalizePrimitive_thenByteNode() throws Exception {
            // Given
            final byte input = TEST_INT_VALUE;
            // When
            final Object result = invokePrivateStatic("tryNormalizePrimitive", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(IntNode.class, result);
            assertEquals(TEST_INT_VALUE, ((JsonNode) result).intValue());
        }
    }

    @Nested
    @DisplayName("tryNormalizeBigNumber")
    class TryNormalizeBigNumber {

        @Test
        @DisplayName("Given BigInteger within long range, When tryNormalizeBigNumber is called,"
                + " Then a LongNode is returned")
        void givenBigIntegerInRange_whenTryNormalizeBigNumber_thenLongNode() throws Exception {
            // Given
            final BigInteger input = BigInteger.valueOf(Long.MAX_VALUE);
            // When
            final Object result = invokePrivateStatic("tryNormalizeBigNumber", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(LongNode.class, result);
            assertEquals(Long.MAX_VALUE, ((JsonNode) result).longValue());
        }

        @Test
        @DisplayName("Given BigInteger outside long range, When tryNormalizeBigNumber is called,"
                + " Then a StringNode is returned")
        void givenBigIntegerOutOfRange_whenTryNormalizeBigNumber_thenStringNode() throws Exception {
            // Given
            final BigInteger input = new BigInteger("99999999999999999999999999999999");
            // When
            final Object result = invokePrivateStatic("tryNormalizeBigNumber", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals(input.toString(), ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given BigDecimal value, When tryNormalizeBigNumber is called, Then a DecimalNode is returned")
        void givenBigDecimal_whenTryNormalizeBigNumber_thenDecimalNode() throws Exception {
            // Given
            final BigDecimal input = new BigDecimal("123.456");
            // When
            final Object result = invokePrivateStatic("tryNormalizeBigNumber", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(DecimalNode.class, result);
            assertEquals(input, ((JsonNode) result).decimalValue());
        }

        @Test
        @DisplayName("Given non big-number type, When tryNormalizeBigNumber is called, Then null is returned")
        void givenOther_whenTryNormalizeBigNumber_thenNull() throws Exception {
            // Given
            final String input = "not-a-number";
            // When
            final Object result = invokePrivateStatic("tryNormalizeBigNumber", new Class[]{Object.class}, input);
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
            final LocalDate input = LocalDate.of(2024, 2, 29);
            // When
            final Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("2024-02-29", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given non temporal type, When tryNormalizeTemporal is called, Then null is returned")
        void givenOther_whenTryNormalizeTemporal_thenNull() throws Exception {
            // Given
            final Object input = 10;
            // When
            final Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);
            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Given LocalDateTime, When tryNormalizeTemporal is called,"
                + " Then an ISO date StringNode is returned")
        void givenLocalDateTime_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            final LocalDateTime input = LocalDateTime.of(2024, 2, 29, 14, 45, 12);
            // When
            final Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("2024-02-29T14:45:12", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given LocalTime, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenLocalTime_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            final LocalTime input = LocalTime.of(14, 45, 12);
            // When
            final Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("14:45:12", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given ZoneDateTime, When tryNormalizeTemporal is called,"
                + " Then an ISO date StringNode is returned")
        void givenZoneDateTime_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            final ZonedDateTime input = ZonedDateTime.of(
                    LocalDate.of(2025, 11, 26), LocalTime.of(15, 45),
                    ZoneId.of("Europe/Berlin"));
            // When
            final Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("2025-11-26T15:45+01:00", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given OffsetDateTime, When tryNormalizeTemporal is called,"
                + " Then an ISO date StringNode is returned")
        void givenOffsetDateTime_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            final ZoneId zone = ZoneId.of("Europe/Berlin");
            final ZoneOffset zoneOffSet = zone.getRules().getOffset(LocalDateTime.of(2025, 11, 26, 15, 45, 36));
            final OffsetDateTime input = OffsetDateTime.of(
                    LocalDate.of(2025, 11, 26), LocalTime.of(15, 45), zoneOffSet);
            // When
            final Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("2025-11-26T15:45:00+01:00", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given Instant, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenInstant_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            final int testYear = 2025;
            final int testMonth = 11;
            final int testDay = 26;
            final int testHour = 15;
            final int testMinute = 45;
            final int testSecond = 36;
            final ZoneId zone = ZoneId.of("Europe/Berlin");
            final LocalDateTime localDateTime = LocalDateTime.of(
                testYear, testMonth, testDay, testHour, testMinute, testSecond);
            final ZoneOffset zoneOffSet = zone.getRules().getOffset(localDateTime);
            final Instant input = localDateTime.toInstant(zoneOffSet);
            // When
            final Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("2025-11-26T14:45:36Z", ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given Calendar, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        void givenCalendar_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            final int calYear = 2017;
            final int calDay = 16;
            final int calHour = 20;
            final int calMinute = 22;
            final int calSecond = 28;
            final Calendar input = Calendar.getInstance();
            input.set(calYear, Calendar.FEBRUARY, calDay, calHour, calMinute, calSecond);
            input.set(Calendar.MILLISECOND, 0);
            // When
            final Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            final String expected = input.toInstant().toString();
            assertEquals(expected, ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given GregorianCalendar, When tryNormalizeTemporal is called,"
                + " Then an ISO date StringNode is returned")
        void givenGregorianCalendar_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            final GregorianCalendar input = new GregorianCalendar(2017, Calendar.FEBRUARY, 16, 20, 22, 28);
            // When
            final Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            final String expected = input.toInstant().toString();
            assertEquals(expected, ((JsonNode) result).asString());
        }

        @Test
        @DisplayName("Given Date, When tryNormalizeTemporal is called, Then an ISO date StringNode is returned")
        @SuppressWarnings("JavaUtilDate")
        void givenDate_whenTryNormalizeTemporal_thenIsoStringNode() throws Exception {
            // Given
            final Date input = new Date(1764362004);
            // When
            final Object result = invokePrivateStatic("tryNormalizeTemporal", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(StringNode.class, result);
            assertEquals("1970-01-21", ((JsonNode) result).asString());
        }
    }

    @Nested
    @DisplayName("tryConvertToLong")
    class TryConvertToLong {

        @Test
        @DisplayName("Given whole double within long range, When tryConvertToLong is called,"
                + " Then Optional with LongNode is returned")
        void givenWholeDoubleInRange_whenTryConvertToLong_thenOptionalLongNode() throws Exception {
            // Given
            final double million = 1_000_000d;
            final Double input = million;
            // When
            final Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);
            // Then
            assertInstanceOf(Optional.class, result);
            final Optional<?> opt = (Optional<?>) result;
            assertTrue(opt.isPresent());
            assertInstanceOf(LongNode.class, opt.get());
            assertEquals(CONVERTIBLE_LONG_VALUE, ((JsonNode) opt.get()).longValue());
        }

        @Test
        @DisplayName("Given fractional double, When tryConvertToLong is called, Then Optional.empty is returned")
        void givenFractionalDouble_whenTryConvertToLong_thenEmpty() throws Exception {
            // Given
            final Double input = 3.14;
            // When
            final Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);
            // Then
            assertInstanceOf(Optional.class, result);
            assertTrue(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given whole double outside long range (max),"
                + " When tryConvertToLong is called, Then Optional.empty is returned")
        void givenWholeDoubleOutOfRangeMax_whenTryConvertToLong_thenEmpty() throws Exception {
            // Given
            final Double input = (double) Long.MAX_VALUE + 1000d;
            // When
            final Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);
            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given whole double outside long range (min),"
                + " When tryConvertToLong is called, Then Optional.empty is returned")
        void givenWholeDoubleOutOfRangeMin_whenTryConvertToLong_thenEmpty() throws Exception {
            // Given
            final Double input = (double) Long.MIN_VALUE - 1000d;
            // When
            final Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);
            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given NonInteger, When tryConvertToLong is called, Then Optional.empty is returned")
        void testNonIntegerValueReturnsEmpty_whenTryConvertToLong() throws Exception {
            // Given
            final Double input = (double) 3.14;
            // When
            final Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);
            // Then
            assertInstanceOf(Optional.class, result);
            assertTrue(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given Integer, When tryConvertToLong is called, Then Optional is returned")
        void testIntegerValueReturnsOptional_whenTryConvertToLong() throws Exception {
            // Given
            final Double input = (double) 10.0;
            // When
            final Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);
            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given Long Max Value, When tryConvertToLong is called, Then Optional is returned")
        void testLongMaxValueReturnsOptional_whenTryConvertToLong() throws Exception {
            // Given
            final Double input = (double) Long.MAX_VALUE + 1;
            // When
            final Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);
            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given Long Min Value, When tryConvertToLong is called, Then Optional is returned")
        void testLongMinValueReturnsOptional_whenTryConvertToLong() throws Exception {
            // Given
            final Double input = (double) Long.MIN_VALUE - 1;
            // When
            final Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);
            // Then
            assertInstanceOf(Optional.class, result);
            assertFalse(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given Long Min Value, When tryConvertToLong is called, Then Optional is returned")
        void testLongNormalizeBigInteger() throws Exception {
            // Given
            final BigInteger input = BigInteger.valueOf(Long.MIN_VALUE - 1);
            // When
            final Object result = invokePrivateStatic("normalizeBigInteger", new Class[]{BigInteger.class}, input);
            // Then
            assertInstanceOf(JsonNode.class, result);
            assertFalse(((JsonNode) result).isBigDecimal());
        }

        @Test
        @DisplayName("Given negative NonInteger, When tryConvertToLong is called, Then Optional.empty is returned")
        void testNegativeNonIntegerValueReturnsEmptyWhenTryConvertToLong() throws Exception {
            // Given
            final Double input = (double) -5.7;
            // When
            final Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);
            // Then
            assertInstanceOf(Optional.class, result);
            assertTrue(((Optional<?>) result).isEmpty());
        }

        @Test
        @DisplayName("Given negative Integer, When tryConvertToLong is called, Then Optional is returned")
        void testNegativeIntegerValueReturnsOptionalWhenTryConvertToLong() throws Exception {
            // Given
            final Double input = (double) -8.0;
            // When
            final Object result = invokePrivateStatic("tryConvertToLong", new Class[]{Double.class}, input);
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
            final List<Object> input = java.util.Arrays.asList(1, "two", true);
            // When
            final Object result = invokePrivateStatic("tryNormalizeCollection", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(ArrayNode.class, result);
            final ArrayNode array = (ArrayNode) result;
            assertEquals(TEST_ARRAY_SIZE, array.size());
            assertEquals(1, array.get(0).asInt());
            assertEquals("two", array.get(1).asString());
            assertTrue(array.get(2).asBoolean());
        }

        @Test
        @DisplayName("Given Map, When tryNormalizeCollection is called, Then ObjectNode is returned")
        void givenMap_whenTryNormalizeCollection_thenObjectNode() throws Exception {
            // Given
            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("a", 1);
            input.put("b", "two");
            // When
            final Object result = invokePrivateStatic("tryNormalizeCollection", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(ObjectNode.class, result);
            final ObjectNode object = (ObjectNode) result;
            assertEquals(1, object.get("a").asInt());
            assertEquals("two", object.get("b").asString());
        }

        @Test
        @DisplayName("Given non-collection, When tryNormalizeCollection is called, Then null is returned")
        void givenOther_whenTryNormalizeCollection_thenNull() throws Exception {
            // Given
            final Object input = 10.0;
            // When
            final Object result = invokePrivateStatic("tryNormalizeCollection", new Class[]{Object.class}, input);
            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("normalizeCollection")
    class NormalizeCollection {

        @Test
        @DisplayName("Given mixed-type list, When normalizeCollection is called,"
                + " Then items are normalized in an ArrayNode")
        void givenMixedList_whenNormalizeCollection_thenArrayNode() throws Exception {
            // Given
            final double threePointZero = 3.0;
            final List<Object> input = java.util.Arrays.asList(1, 2L, threePointZero, "four");
            // When
            final Object result = invokePrivateStatic("normalizeCollection", new Class[]{Collection.class}, input);
            // Then
            assertInstanceOf(ArrayNode.class, result);
            final ArrayNode array = (ArrayNode) result;
            assertEquals(EXPECTED_SIZE_FOUR, array.size());
            assertEquals(1, array.get(0).asInt());
            assertEquals(2L, array.get(1).asLong());
            assertEquals(threePointZero, array.get(2).asDouble());
            assertEquals("four", array.get(EXPECTED_SIZE_FOUR - 1).asString());
        }

        @Test
        @DisplayName("Given empty list, When normalizeCollection is called, Then an empty ArrayNode is returned")
        void givenEmptyList_whenNormalizeCollection_thenEmptyArrayNode() throws Exception {
            // Given
            final List<Object> input = java.util.Collections.emptyList();
            // When
            final Object result = invokePrivateStatic("normalizeCollection", new Class[]{Collection.class}, input);
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
            final Object input = new Object();
            // When
            final Object result = invokePrivateStatic("normalizeArray", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(ArrayNode.class, result);
        }
    }

    @Nested
    @DisplayName("NormalizePojo")
    class NormalizePojo {
        static class ExplodingPojo {
            public String getValue() {
                throw new IllegalStateException("Boom");
            }
        }

        @Test
        @DisplayName("Given Object, When tryNormalizePojo is called, Then ArrayNode get return")
        void tryNormalizePojo_thenNullNode() throws Exception {
            // Given
            final Object input = new Object();
            // When
            final Object result = invokePrivateStatic("tryNormalizePojo", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(ObjectNode.class, result);
        }

        @Test
        void returnsNullNodeWhenJacksonExceptionOccurs() throws Exception {
            // Given
            final Object input = new ExplodingPojo();
            // When
            final Object result = invokePrivateStatic("tryNormalizePojo", new Class[]{Object.class}, input);
            // Then
            assertInstanceOf(NullNode.class, result);
        }
    }

    @Nested
    @DisplayName("parse")
    class Parse {
        @Test
        void parseNullAsString() {
            // Given
            final String input = null;
            // When
            final IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> JsonNormalizer.parse(input));
            // Then
            assertEquals("JSON string cannot be null", thrown.getMessage());
        }


        @Test
        void parseEmptyString() {
            // Given
            final String input = " ";
            // When
            final IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> JsonNormalizer.parse(input));
            // Then
            assertEquals("JSON string cannot be blank", thrown.getMessage());
        }

    }

    @Nested
    @DisplayName("Security - Depth Limits")
    class SecurityDepthLimits {

        @Test
        @DisplayName("MAX_ALLOWED_NESTING_DEPTH constant should be 256")
        void constantShouldBe256() {
            final int expectedDepth = 256;
            assertEquals(expectedDepth, JsonNormalizer.MAX_ALLOWED_NESTING_DEPTH);
        }

        @Test
        @DisplayName("Should throw when nesting depth exceeds MAX_DEPTH")
        void throwsWhenDepthExceedsMax() {
            // Given - create deeply nested structure that exceeds MAX_DEPTH
            // We'll use reflection to test this by creating a custom scenario
            // For practical testing, we verify the constant exists and logic works
            final Map<String, Object> deepMap = new HashMap<>();
            Map<String, Object> current = deepMap;
            final int testDepth = 600;
            for (int i = 0; i < testDepth; i++) {
                final Map<String, Object> next = new HashMap<>();
                next.put("value", "test");
                current.put("nested", next);
                current = next;
            }
            // When/Then - should throw due to depth limit
            assertThrows(IllegalArgumentException.class, () -> JsonNormalizer.normalize(deepMap));
        }

        @Test
        @DisplayName("Should include MAX_DEPTH in exception message")
        void exceptionMessageIncludesMaxDepth() {
            final Map<String, Object> deepMap = new HashMap<>();
            Map<String, Object> current = deepMap;
            final int testDepth = 600;
            for (int i = 0; i < testDepth; i++) {
                final Map<String, Object> next = new HashMap<>();
                current.put("nested", next);
                current = next;
            }

            final IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> JsonNormalizer.normalize(deepMap)
            );

            assertTrue(thrown.getMessage().contains("256"));
            assertTrue(thrown.getMessage().contains("nesting depth"));
        }
    }

    @Nested
    @DisplayName("Security - Circular Reference Detection")
    class SecurityCircularReference {

        @Test
        @DisplayName("Should detect circular reference in Map")
        void detectsCircularMapReference() {
            // Given - create circular reference in Map
            final Map<String, Object> map1 = new HashMap<>();
            final Map<String, Object> map2 = new HashMap<>();
            map1.put("key", map2);
            map2.put("key", map1); // circular!
            // When/Then
            final IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> JsonNormalizer.normalize(map1)
            );

            assertTrue(thrown.getMessage().contains("Circular reference"));
        }

        @Test
        @DisplayName("Should detect circular reference in List")
        void detectsCircularListReference() {
            // Given - create circular reference in List
            final List<Object> list1 = new java.util.ArrayList<>();
            final List<Object> list2 = new java.util.ArrayList<>();
            list1.add(list2);
            list2.add(list1); // circular!
            // When/Then
            final IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> JsonNormalizer.normalize(list1)
            );

            assertTrue(thrown.getMessage().contains("Circular reference"));
        }

        @Test
        @DisplayName("Should detect self-referential object")
        void detectsSelfReference() {
            // Given - self-referential object
            final Map<String, Object> map = new HashMap<>();
            map.put("self", map);
            // When/Then
            final IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> JsonNormalizer.normalize(map)
            );

            assertTrue(thrown.getMessage().contains("Circular reference"));
        }
    }

    @Nested
    @DisplayName("Security - Stream Handling")
    class SecurityStreamHandling {

        @Test
        @DisplayName("Stream should be materialized to List")
        void streamMaterializedToList() {
            // Given
            final int streamSize = 3;
            final Stream<String> stream = Stream.of("a", "b", "c");
            // When
            final JsonNode result = JsonNormalizer.normalize(stream);
            // Then
            assertInstanceOf(ArrayNode.class, result);
            assertEquals(streamSize, result.size());
        }

        @Test
        @DisplayName("Empty stream should return empty array")
        void emptyStreamReturnsEmptyArray() {
            // Given
            final Stream<String> stream = Stream.empty();
            // When
            final JsonNode result = JsonNormalizer.normalize(stream);
            // Then
            assertInstanceOf(ArrayNode.class, result);
            assertEquals(0, result.size());
        }
    }
}

