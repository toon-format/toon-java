package dev.toonformat.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JToonRaceConditionTest {

    @Test
    @DisplayName("Should be thread-safe when encoding and decoding concurrently")
    void concurrentEncodeDecode() throws InterruptedException, ExecutionException {
        int threadCount = 20;
        int iterationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("name", "JToon");
        input.put("version", "1.0.0");
        input.put("tags", List.of("java", "json", "toon"));
        input.put("active", true);
        
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("author", "dev");
        metadata.put("stars", 100);
        metadata.put("created", java.time.LocalDateTime.now());
        input.put("metadata", metadata);

        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount * iterationsPerThread; i++) {
            futures.add(executor.submit(() -> {
                String encoded = JToon.encode(input);
                Object decoded = JToon.decode(encoded);
                
                // When decoding, LocalDateTime becomes a String
                // We use toString check for other fields and manual check for metadata
                Map<String, Object> decodedMap = (Map<String, Object>) decoded;
                assertEquals(input.get("name"), decodedMap.get("name"));
                assertEquals(input.get("version"), decodedMap.get("version"));
                assertEquals(input.get("active"), decodedMap.get("active"));
                
                Map<String, Object> decodedMetadata = (Map<String, Object>) decodedMap.get("metadata");
                assertEquals("dev", decodedMetadata.get("author"));
                assertEquals(100L, ((Number) decodedMetadata.get("stars")).longValue());
                
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should handle different objects concurrently without interference")
    void concurrentDifferentObjects() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        int iterations = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                Map<String, Object> obj = Map.of("key", "value" + index);
                String encoded = JToon.encode(obj);
                Map<String, Object> decoded = (Map<String, Object>) JToon.decode(encoded);
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
