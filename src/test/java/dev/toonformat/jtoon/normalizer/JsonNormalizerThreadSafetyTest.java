package dev.toonformat.jtoon.normalizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
class JsonNormalizerThreadSafetyTest {

    @RepeatedTest(100)
    @DisplayName("JsonNormalizer should be thread-safe when normalizing complex objects")
    void normalizeThreadSafety() {
        // Given
        String id = UUID.randomUUID().toString();
        Map<String, Object> input = Map.of(
            "id", id,
            "timestamp", LocalDateTime.now(),
            "tags", List.of("a", "b", "c"),
            "nested", Map.of("key", "value")
        );

        // When
        JsonNode normalized = JsonNormalizer.normalize(input);

        // Then
        assertNotNull(normalized);
        assertTrue(normalized.isObject());
        assertTrue(normalized.has("id"));
        assertEquals(normalized.get("id").asString(), id);
    }

    @RepeatedTest(100)
    @DisplayName("JsonNormalizer should be thread-safe when parsing JSON strings")
    void parseThreadSafety() {
        // Given
        String id = UUID.randomUUID().toString();
        String json = "{\"id\":\"" + id + "\",\"active\":true}";

        // When
        JsonNode parsed = JsonNormalizer.parse(json);

        // Then
        assertNotNull(parsed);
        assertTrue(parsed.isObject());
        assertEquals(parsed.get("id").asString(), id);
    }
}
