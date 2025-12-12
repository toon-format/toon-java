package dev.toonformat.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for round-trip encode/decode symmetry.
 * Verifies that decode(encode(object)) preserves the original data structure.
 */
@Tag("integration")
public class RoundTripTest {

    @Nested
    @DisplayName("Primitives Round-Trip")
    class PrimitivesRoundTrip {

        @Test
        @DisplayName("should preserve null values")
        void testNullRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("value", null);

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            assertEquals(data, decoded);
        }

        @Test
        @DisplayName("should preserve boolean values")
        void testBooleanRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("active", true);
            data.put("enabled", false);

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            assertEquals(data, decoded);
        }

        @Test
        @DisplayName("should preserve integer values")
        void testIntegerRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", 42);
            data.put("zero", 0);
            data.put("negative", -100);

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            @SuppressWarnings("unchecked")
            Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            // Integers decode as Long, so compare numeric values
            assertEquals(42L, decodedMap.get("count"));
            assertEquals(0L, decodedMap.get("zero"));
            assertEquals(-100L, decodedMap.get("negative"));
        }

        @Test
        @DisplayName("should preserve floating point values")
        void testFloatRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("pi", 3.14);
            data.put("price", 99.99);

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            @SuppressWarnings("unchecked")
            Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            assertEquals(3.14, (Double) decodedMap.get("pi"), 0.0001);
            assertEquals(99.99, (Double) decodedMap.get("price"), 0.0001);
        }

        @Test
        @DisplayName("should preserve string values")
        void testStringRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "Ada");
            data.put("note", "hello, world");
            data.put("empty", "");

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            assertEquals(data, decoded);
        }

        @Test
        @DisplayName("should preserve strings with special characters")
        void testSpecialCharacterStringRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("text", "line1\nline2");
            data.put("path", "C:\\Users\\Documents");
            data.put("quote", "He said \"hello\"");

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            assertEquals(data, decoded);
        }
    }

    @Nested
    @DisplayName("Arrays Round-Trip")
    class ArraysRoundTrip {

        @Test
        @DisplayName("should preserve primitive arrays")
        void testPrimitiveArrayRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("tags", Arrays.asList("reading", "gaming", "coding"));

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            assertEquals(data, decoded);
        }

        @Test
        @DisplayName("should preserve tabular arrays")
        void testTabularArrayRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();

            Map<String, Object> user1 = new LinkedHashMap<>();
            user1.put("id", 1);
            user1.put("name", "Alice");
            user1.put("role", "admin");

            Map<String, Object> user2 = new LinkedHashMap<>();
            user2.put("id", 2);
            user2.put("name", "Bob");
            user2.put("role", "user");

            data.put("users", Arrays.asList(user1, user2));

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            @SuppressWarnings("unchecked")
            Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            @SuppressWarnings("unchecked")
            List<Object> users = (List<Object>) decodedMap.get("users");
            assertEquals(2, users.size());

            @SuppressWarnings("unchecked")
            Map<String, Object> decodedUser1 = (Map<String, Object>) users.get(0);
            assertEquals(1L, decodedUser1.get("id"));  // Integers decode as Long
            assertEquals("Alice", decodedUser1.get("name"));
            assertEquals("admin", decodedUser1.get("role"));
        }

        @Test
        @DisplayName("should preserve empty arrays")
        void testEmptyArrayRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", List.of());

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            assertEquals(data, decoded);
        }
    }

    @Nested
    @DisplayName("Nested Objects Round-Trip")
    class NestedObjectsRoundTrip {

        @Test
        @DisplayName("should preserve nested objects")
        void testNestedObjectRoundTrip() {
            Map<String, Object> contact = new LinkedHashMap<>();
            contact.put("email", "ada@example.com");
            contact.put("phone", "555-1234");

            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", 123);
            user.put("name", "Ada");
            user.put("contact", contact);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", user);

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            @SuppressWarnings("unchecked")
            Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedUser = (Map<String, Object>) decodedMap.get("user");
            assertEquals(123L, decodedUser.get("id"));  // Integers decode as Long
            assertEquals("Ada", decodedUser.get("name"));
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedContact = (Map<String, Object>) decodedUser.get("contact");
            assertEquals("ada@example.com", decodedContact.get("email"));
            assertEquals("555-1234", decodedContact.get("phone"));
        }

        @Test
        @DisplayName("should preserve deeply nested structures")
        void testDeeplyNestedRoundTrip() {
            Map<String, Object> level3 = new LinkedHashMap<>();
            level3.put("value", 42);

            Map<String, Object> level2 = new LinkedHashMap<>();
            level2.put("nested", level3);

            Map<String, Object> level1 = new LinkedHashMap<>();
            level1.put("nested", level2);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("nested", level1);

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            // Navigate through nested structure and verify
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedLevel1 = (Map<String, Object>) decodedMap.get("nested");
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedLevel2 = (Map<String, Object>) decodedLevel1.get("nested");
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedLevel3 = (Map<String, Object>) decodedLevel2.get("nested");
            assertEquals(42L, decodedLevel3.get("value"));  // Integers decode as Long
        }
    }

    @Nested
    @DisplayName("Complex Structures Round-Trip")
    class ComplexStructuresRoundTrip {

        @Test
        @DisplayName("should preserve mixed root-level content")
        void testMixedContentRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", 123);
            data.put("name", "Ada");
            data.put("tags", Arrays.asList("dev", "admin"));
            data.put("active", true);

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            @SuppressWarnings("unchecked")
            Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            assertEquals(123L, decodedMap.get("id"));  // Integers decode as Long
            assertEquals("Ada", decodedMap.get("name"));
            assertEquals(true, decodedMap.get("active"));
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) decodedMap.get("tags");
            assertEquals(Arrays.asList("dev", "admin"), tags);
        }

        @Test
        @DisplayName("should preserve objects with nested arrays and objects")
        void testComplexNestedRoundTrip() {
            Map<String, Object> contact = new LinkedHashMap<>();
            contact.put("email", "ada@example.com");

            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", 123);
            user.put("name", "Ada");
            user.put("tags", Arrays.asList("dev", "admin"));
            user.put("contact", contact);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", user);

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            @SuppressWarnings("unchecked")
            Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedUser = (Map<String, Object>) decodedMap.get("user");
            assertEquals(123L, decodedUser.get("id"));  // Integers decode as Long
            assertEquals("Ada", decodedUser.get("name"));
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) decodedUser.get("tags");
            assertEquals(Arrays.asList("dev", "admin"), tags);
            @SuppressWarnings("unchecked")
            Map<String, Object> decodedContact = (Map<String, Object>) decodedUser.get("contact");
            assertEquals("ada@example.com", decodedContact.get("email"));
        }
    }

    @Nested
    @DisplayName("Delimiter Options Round-Trip")
    class DelimiterOptionsRoundTrip {

        @Test
        @DisplayName("should preserve data with tab delimiter")
        void testTabDelimiterRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("tags", Arrays.asList("a", "b", "c"));

            EncodeOptions encodeOpts = new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE);
            DecodeOptions decodeOpts = new DecodeOptions(2, Delimiter.TAB, true, PathExpansion.OFF);

            String toon = JToon.encode(data, encodeOpts);
            Object decoded = JToon.decode(toon, decodeOpts);

            assertEquals(data, decoded);
        }

        @Test
        @DisplayName("should preserve data with pipe delimiter")
        void testPipeDelimiterRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("tags", Arrays.asList("a", "b", "c"));

            EncodeOptions encodeOpts = new EncodeOptions(2, Delimiter.PIPE, false, KeyFolding.OFF, Integer.MAX_VALUE);
            DecodeOptions decodeOpts = new DecodeOptions(2, Delimiter.PIPE, true, PathExpansion.OFF);

            String toon = JToon.encode(data, encodeOpts);
            Object decoded = JToon.decode(toon, decodeOpts);

            assertEquals(data, decoded);
        }
    }

    @Nested
    @DisplayName("JSON Round-Trip")
    class JsonRoundTrip {

        @Test
        @DisplayName("should preserve data through JSON intermediary")
        void testJsonRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", 123);
            data.put("name", "Ada");
            data.put("tags", Arrays.asList("dev", "admin"));

            String toon = JToon.encode(data);
            String json = JToon.decodeToJson(toon);
            String toon2 = JToon.encodeJson(json);
            Object decoded = JToon.decode(toon2);

            @SuppressWarnings("unchecked")
            Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            assertEquals(123L, decodedMap.get("id"));  // Integers decode as Long
            assertEquals("Ada", decodedMap.get("name"));
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) decodedMap.get("tags");
            assertEquals(Arrays.asList("dev", "admin"), tags);
        }
    }

    @Nested
    @DisplayName("Edge Cases Round-Trip")
    class EdgeCasesRoundTrip {

        @Test
        @DisplayName("should preserve empty object")
        void testEmptyObjectRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            // Empty object encodes to empty string, which decodes to empty object
            assertEquals(Collections.emptyMap(), decoded);
        }

        @Test
        @DisplayName("should preserve special character keys")
        void testSpecialKeyRoundTrip() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("order:id", 42);
            data.put("full name", "Alice");

            String toon = JToon.encode(data);
            Object decoded = JToon.decode(toon);

            @SuppressWarnings("unchecked")
            Map<String, Object> decodedMap = (Map<String, Object>) decoded;
            assertEquals(42L, decodedMap.get("order:id"));  // Integers decode as Long
            assertEquals("Alice", decodedMap.get("full name"));
        }
    }
}
