package dev.toonformat.jtoon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for round-trip encode/decode symmetry.
 * Verifies that decode(encode(object)) preserves the original data structure.
 */
@Tag("integration")
class RoundTripTest {

    private static final int TEST_COUNT = 42;
    private static final int TEST_NEGATIVE = -100;
    private static final double TEST_PI = 3.14;
    private static final double TEST_PRICE = 99.99;
    private static final double TEST_DELTA = 0.0001;
    private static final int TEST_USER_ID = 123;
    private static final int SPECIAL_KEY_VALUE = 42;

    @Nested
    @DisplayName("Primitives Round-Trip")
    class PrimitivesRoundTrip {

        @Test
        @DisplayName("should preserve null values")
        void testNullRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", null);

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            assertEquals(data, decoded);
        }

        @Test
        @DisplayName("should preserve boolean values")
        void testBooleanRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("active", true);
            data.put("enabled", false);

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            assertEquals(data, decoded);
        }

        @Test
        @DisplayName("should preserve integer values")
        void testIntegerRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", TEST_COUNT);
            data.put("zero", 0);
            data.put("negative", TEST_NEGATIVE);

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            // Integers decode as Long, so compare numeric values
            assertEquals((long) TEST_COUNT, decodedMap.get("count"));
            assertEquals(0L, decodedMap.get("zero"));
            assertEquals((long) TEST_NEGATIVE, decodedMap.get("negative"));
        }

        @Test
        @DisplayName("should preserve floating point values")
        void testFloatRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("pi", TEST_PI);
            data.put("price", TEST_PRICE);

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            assertEquals(TEST_PI, (Double) decodedMap.get("pi"), TEST_DELTA);
            assertEquals(TEST_PRICE, (Double) decodedMap.get("price"), TEST_DELTA);
        }

        @Test
        @DisplayName("should preserve string values")
        void testStringRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "Ada");
            data.put("note", "hello, world");
            data.put("empty", "");

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            assertEquals(data, decoded);
        }

        @Test
        @DisplayName("should preserve strings with special characters")
        void testSpecialCharacterStringRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("text", "line1\nline2");
            data.put("path", "C:\\Users\\Documents");
            data.put("quote", "He said \"hello\"");

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            assertEquals(data, decoded);
        }
    }

    @Nested
    @DisplayName("Arrays Round-Trip")
    class ArraysRoundTrip {

        @Test
        @DisplayName("should preserve primitive arrays")
        void testPrimitiveArrayRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("tags", Arrays.asList("reading", "gaming", "coding"));

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            assertEquals(data, decoded);
        }

        @Test
        @DisplayName("should preserve tabular arrays")
        void testTabularArrayRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();

            final Map<String, Object> user1 = new LinkedHashMap<>();
            user1.put("id", 1);
            user1.put("name", "Alice");
            user1.put("role", "admin");

            final Map<String, Object> user2 = new LinkedHashMap<>();
            user2.put("id", 2);
            user2.put("name", "Bob");
            user2.put("role", "user");

            data.put("users", Arrays.asList(user1, user2));

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            @SuppressWarnings("unchecked")
            final List<Object> users = (List<Object>) decodedMap.get("users");
            assertEquals(2, users.size());

            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedUser1 = (Map<String, Object>) users.get(0);
            assertEquals(1L, decodedUser1.get("id"));  // Integers decode as Long
            assertEquals("Alice", decodedUser1.get("name"));
            assertEquals("admin", decodedUser1.get("role"));
        }

        @Test
        @DisplayName("should preserve empty arrays")
        void testEmptyArrayRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", List.of());

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            assertEquals(data, decoded);
        }
    }

    @Nested
    @DisplayName("Nested Objects Round-Trip")
    class NestedObjectsRoundTrip {

        @Test
        @DisplayName("should preserve nested objects")
        void testNestedObjectRoundTrip() {
            // Given
            final Map<String, Object> contact = new LinkedHashMap<>();
            contact.put("email", "ada@example.com");
            contact.put("phone", "555-1234");

            final Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", TEST_USER_ID);
            user.put("name", "Ada");
            user.put("contact", contact);

            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", user);

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedUser = (Map<String, Object>) decodedMap.get("user");
            assertEquals((long) TEST_USER_ID, decodedUser.get("id"));  // Integers decode as Long
            assertEquals("Ada", decodedUser.get("name"));
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedContact = (Map<String, Object>) decodedUser.get("contact");
            assertEquals("ada@example.com", decodedContact.get("email"));
            assertEquals("555-1234", decodedContact.get("phone"));
        }

        @Test
        @DisplayName("should preserve deeply nested structures")
        void testDeeplyNestedRoundTrip() {
            // Given
            final Map<String, Object> level3 = new LinkedHashMap<>();
            level3.put("value", TEST_COUNT);

            final Map<String, Object> level2 = new LinkedHashMap<>();
            level2.put("nested", level3);

            final Map<String, Object> level1 = new LinkedHashMap<>();
            level1.put("nested", level2);

            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("nested", level1);

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            // Navigate through nested structure and verify
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedLevel1 = (Map<String, Object>) decodedMap.get("nested");
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedLevel2 = (Map<String, Object>) decodedLevel1.get("nested");
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedLevel3 = (Map<String, Object>) decodedLevel2.get("nested");
            assertEquals((long) TEST_COUNT, decodedLevel3.get("value"));  // Integers decode as Long
        }
    }

    @Nested
    @DisplayName("Complex Structures Round-Trip")
    class ComplexStructuresRoundTrip {

        @Test
        @DisplayName("should preserve mixed root-level content")
        void testMixedContentRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            final int expectedUserId = 123;
            data.put("id", expectedUserId);
            data.put("name", "Ada");
            data.put("tags", Arrays.asList("dev", "admin"));
            data.put("active", true);

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            assertEquals((long) TEST_USER_ID, decodedMap.get("id"));  // Integers decode as Long
            assertEquals("Ada", decodedMap.get("name"));
            @SuppressWarnings("unchecked")
            final List<Object> tags = (List<Object>) decodedMap.get("tags");
            assertEquals(Arrays.asList("dev", "admin"), tags);
        }
    }

    @Nested
    @DisplayName("Delimiter Options Round-Trip")
    class DelimiterOptionsRoundTrip {

        @Test
        @DisplayName("should preserve data with tab delimiter")
        void testTabDelimiterRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("tags", Arrays.asList("a", "b", "c"));

            final EncodeOptions encodeOpts = new EncodeOptions(2, Delimiter.TAB, KeyFolding.OFF,
                    Integer.MAX_VALUE);
            final DecodeOptions decodeOpts = new DecodeOptions(2, Delimiter.TAB, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

            // When
            final String toon = JToon.encode(data, encodeOpts);
            final Object decoded = JToon.decode(toon, decodeOpts);

            // Then
            assertEquals(data, decoded);
        }

        @Test
        @DisplayName("should preserve data with pipe delimiter")
        void testPipeDelimiterRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("tags", Arrays.asList("a", "b", "c"));

            final EncodeOptions encodeOpts = new EncodeOptions(2, Delimiter.PIPE, KeyFolding.OFF,
                    Integer.MAX_VALUE);
            final DecodeOptions decodeOpts = new DecodeOptions(2, Delimiter.PIPE, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

            // When
            final String toon = JToon.encode(data, encodeOpts);
            final Object decoded = JToon.decode(toon, decodeOpts);

            // Then
            assertEquals(data, decoded);
        }
    }

    @Nested
    @DisplayName("JSON Round-Trip")
    class JsonRoundTrip {

        @Test
        @DisplayName("should preserve data through JSON intermediary")
        void testJsonRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", TEST_USER_ID);
            data.put("name", "Ada");
            data.put("tags", Arrays.asList("dev", "admin"));

            // When
            final String toon = JToon.encode(data);
            final String json = JToon.decodeToJson(toon);
            final String toon2 = JToon.encodeJson(json);
            final Object decoded = JToon.decode(toon2);


            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            assertEquals((long) TEST_USER_ID, decodedMap.get("id"));  // Integers decode as Long
            assertEquals("Ada", decodedMap.get("name"));
            @SuppressWarnings("unchecked")
            final List<Object> tags = (List<Object>) decodedMap.get("tags");
            assertEquals(Arrays.asList("dev", "admin"), tags);
        }
    }

    @Nested
    @DisplayName("Edge Cases Round-Trip")
    class EdgeCasesRoundTrip {

        @Test
        @DisplayName("should preserve empty object")
        void testEmptyObjectRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            // Empty object encodes to empty string, which decodes to empty object
            assertEquals(Collections.emptyMap(), decoded);
        }

        @Test
        @DisplayName("should preserve special character keys")
        void testSpecialKeyRoundTrip() {
            // Given
            final Map<String, Object> data = new LinkedHashMap<>();
            data.put("order:id", SPECIAL_KEY_VALUE);
            data.put("full name", "Alice");

            // When
            final String toon = JToon.encode(data);
            final Object decoded = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            assertEquals((long) SPECIAL_KEY_VALUE, decodedMap.get("order:id"));  // Integers decode as Long
            assertEquals("Alice", decodedMap.get("full name"));
        }
    }
}
