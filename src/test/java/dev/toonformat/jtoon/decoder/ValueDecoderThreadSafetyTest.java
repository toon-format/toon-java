package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Execution(ExecutionMode.CONCURRENT)
class ValueDecoderThreadSafetyTest {

    @RepeatedTest(100)
    @DisplayName("ValueDecoder should be thread-safe when decoding TOON strings")
    void decodeThreadSafety() {
        // Given
        String id = UUID.randomUUID().toString();
        String toon = "id: " + id + "\n" +
            "tags[3]: a, b, c\n" +
            "meta:\n" +
            "  active: true\n" +
            "  score: 42";

        // When
        Object decoded = ValueDecoder.decode(toon, DecodeOptions.DEFAULT);

        // Then
        assertInstanceOf(Map.class, decoded);
        Map<?, ?> map = (Map<?, ?>) decoded;
        assertEquals(id, map.get("id"));
        assertEquals(List.of("a", "b", "c"), map.get("tags"));

        Map<?, ?> meta = (Map<?, ?>) map.get("meta");
        assertEquals(true, meta.get("active"));
        assertEquals(42L, meta.get("score"));
    }
}
