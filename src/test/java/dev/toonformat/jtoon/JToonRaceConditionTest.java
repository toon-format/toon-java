package dev.toonformat.jtoon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JToonRaceConditionTest {

    private static final int AWAIT_TERMINATION_SECONDS = 10;
    private static final int STARS_VALUE = 100;

    @Test
    @DisplayName("Should be thread-safe when encoding and decoding concurrently")
    @SuppressWarnings("unchecked")
    void concurrentEncodeDecode() throws InterruptedException, ExecutionException {
        final int threadCount = 20;
        final int iterationsPerThread = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        final Map<String, Object> input = new LinkedHashMap<>();
        input.put("name", "JToon");
        input.put("version", "1.0.0");
        input.put("tags", List.of("java", "json", "toon"));
        input.put("active", true);
        
        final Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("author", "dev");
        metadata.put("stars", STARS_VALUE);
        metadata.put("created", java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        input.put("metadata", metadata);

        final List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount * iterationsPerThread; i++) {
            futures.add(executor.submit(() -> {
                final String encoded = JToon.encode(input);
                final Object decoded = JToon.decode(encoded);
                
                // When decoding, LocalDateTime becomes a String
                // We use toString check for other fields and manual check for metadata
                final Map<String, Object> decodedMap = (Map<String, Object>) decoded;
                assertEquals(input.get("name"), decodedMap.get("name"));
                assertEquals(input.get("version"), decodedMap.get("version"));
                assertEquals(input.get("active"), decodedMap.get("active"));
                
                final Map<String, Object> decodedMetadata = (Map<String, Object>) decodedMap.get("metadata");
                assertEquals("dev", decodedMetadata.get("author"));
                assertEquals((long) STARS_VALUE, ((Number) decodedMetadata.get("stars")).longValue());
                
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
        if (!executor.awaitTermination(AWAIT_TERMINATION_SECONDS, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should handle different objects concurrently without interference")
    @SuppressWarnings("unchecked")
    void concurrentDifferentObjects() throws InterruptedException, ExecutionException {
        final int threadCount = 10;
        final int iterations = 1000;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        final List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                final Map<String, Object> obj = Map.of("key", "value" + index);
                final String encoded = JToon.encode(obj);
                final Map<String, Object> decoded = (Map<String, Object>) JToon.decode(encoded);
                assertEquals("value" + index, decoded.get("key"));
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
    }
}
