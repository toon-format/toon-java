package dev.toonformat.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class JToonDecodeToMapTest {

    @Nested
    @DisplayName("Valid inputs")
    class ValidInputs {

        @Test
        @DisplayName("should return a non-empty Map")
        void simpleMapDecode() {
            String toon = """
                    id: 123
                    name: Ada
                    active: true
                    """;
            Map<String, Object> map = JToon.decodeToMap(toon);

            assertNotNull(map);
            assertFalse(map.isEmpty());
            assertNotNull(map.get("id"));
            assertNotNull(map.get("name"));
            assertNotNull(map.get("active"));
            assertEquals(123L, map.get("id"));
            assertEquals("Ada", map.get("name"));
            assertEquals(true, map.get("active"));
        }
    }

    @Nested
    @DisplayName("Invalid or edge cases")
    class InvalidInputs {

        @Test
        @DisplayName("should return an empty Map for null input")
        void nullInput() {
            Map<String, Object> map = JToon.decodeToMap(null);
            assertNotNull(map);
            assertTrue(map.isEmpty());
        }

        @Test
        @DisplayName("should return an empty Map for empty input")
        void emptyInput() {
            Map<String, Object> map1 = JToon.decodeToMap("");
            Map<String, Object> map2 = JToon.decodeToMap(" ");
            assertNotNull(map1);
            assertTrue(map1.isEmpty());
            assertNotNull(map2);
            assertTrue(map2.isEmpty());
        }

        @Test
        @DisplayName("should return an empty Map for invalid TOON string")
        void invalidInput() {
            String toon = "This String is invalid";
            Map<String, Object> map = JToon.decodeToMap(toon);
            assertNotNull(map);
            assertTrue(map.isEmpty());
        }

        @Test
        @DisplayName("should return an empty map for primitive inputs")
        void primitiveInputs() {
            assertTrue(JToon.decodeToMap("123").isEmpty());
            assertTrue(JToon.decodeToMap("12.34").isEmpty());
            assertTrue(JToon.decodeToMap("\"Hello\"").isEmpty());
            assertTrue(JToon.decodeToMap("true").isEmpty());
        }

        @Test
        @DisplayName("should return an empty Map for a non-map result")
        void notAMap() {
            String toon = """
                    [3]{name,age,active}:
                      Mark,31,true
                      Adam,20,true
                      Elly,45,false
                    """;
            Map<String, Object> map = JToon.decodeToMap(toon);
            assertNotNull(map);
            assertTrue(map.isEmpty());
        }
    }

    @Nested
    @DisplayName("Nested Objects")
    class NestedObjects {

        @Test
        @DisplayName("should decode nested objects")
        void nestedObjects() {
            String toon = """
                    user:
                      id: 123
                      contact:
                        email: ada@example.com
                        phone: "555-1234"
                    """;
            Map<String, Object> map = JToon.decodeToMap(toon);
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) map.get("user");
            @SuppressWarnings("unchecked")
            Map<String, Object> contact = (Map<String, Object>) user.get("contact");
            assertEquals("ada@example.com", contact.get("email"));
            assertEquals("555-1234", contact.get("phone"));
        }

        @Test
        @DisplayName("should decode object with nested arrays")
        void nestedArrays() {
            String toon = """
                    user:
                      id: 123
                      name: Ada
                      tags[2]: dev,admin
                    """;
            Map<String, Object> map = JToon.decodeToMap(toon);
            assertNotNull(map);
            assertFalse(map.isEmpty());
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) map.get("user");
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) user.get("tags");
            assertEquals(2, tags.size());
        }
    }
}