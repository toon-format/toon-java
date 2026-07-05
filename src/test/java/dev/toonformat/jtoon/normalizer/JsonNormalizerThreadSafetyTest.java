package dev.toonformat.jtoon.normalizer;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import tools.jackson.databind.JsonNode;

@Execution(ExecutionMode.CONCURRENT)
class JsonNormalizerThreadSafetyTest {

    private static final int TEST_REPETITIONS = 100;

    @RepeatedTest(TEST_REPETITIONS)
    @DisplayName("JsonNormalizer should be thread-safe when normalizing complex objects")
    void normalizeThreadSafety() {
        // Given
        final String id = UUID.randomUUID().toString();
        final Map<String, Object> input = Map.of(
            "id", id,
            "timestamp", LocalDateTime.now(ZoneOffset.UTC),
            "tags", List.of("a", "b", "c"),
            "nested", Map.of("key", "value")
        );

        // When
        final JsonNode normalized = JsonNormalizer.normalize(input);

        // Then
        assertNotNull(normalized);
        assertTrue(normalized.isObject());
        assertTrue(normalized.has("id"));
        assertEquals(normalized.get("id").asString(), id);
    }

    @RepeatedTest(TEST_REPETITIONS)
    @DisplayName("JsonNormalizer should be thread-safe when parsing JSON strings")
    void parseThreadSafety() {
        // Given
        final String id = UUID.randomUUID().toString();
        final String json = "{\"id\":\"" + id + "\",\"active\":true}";

        // When
        final JsonNode parsed = JsonNormalizer.parse(json);

        // Then
        assertNotNull(parsed);
        assertTrue(parsed.isObject());
        assertEquals(parsed.get("id").asString(), id);
    }
}
