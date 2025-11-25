package dev.toonformat.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class JToonDecodeToMapTest {

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

    @Test
    @DisplayName("should return an empty Map for null input")
    void nullInput() {
        Map<String, Object> map = JToon.decodeToMap(null);
        assertNotNull(map);
        assertTrue(map.isEmpty());
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
