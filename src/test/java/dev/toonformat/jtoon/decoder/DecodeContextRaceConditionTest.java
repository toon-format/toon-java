package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.JToon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class DecodeContextRaceConditionTest {

    @Test
    @DisplayName("Should be thread-safe when decoding multiple inputs concurrently")
    void concurrentDecoding() throws InterruptedException, ExecutionException {
        int threadCount = 20;
        int iterationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        String toonInput = new StringBuilder()
            .append("name: JToon\n")
            .append("version: 1.0.0\n")
            .append("tags[3]:\n")
            .append("  - java\n")
            .append("  - json\n")
            .append("  - toon\n")
            .append("metadata:\n")
            .append("  author: dev\n")
            .append("  active: true")
            .toString();

        final List<Future<Object>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount * iterationsPerThread; i++) {
            futures.add(executor.submit(() -> JToon.decode(toonInput)));
        }

        for (Future<Object> future : futures) {
            Object result = future.get();
            if (!(result instanceof Map)) {
                fail("Result should be a Map");
            }
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("JToon", map.get("name"));
            assertEquals("1.0.0", map.get("version"));
            assertEquals(true, ((Map<String, Object>) map.get("metadata")).get("active"));
        }

        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should handle different inputs concurrently without interference")
    void concurrentDifferentInputs() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        final List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                String input = "key: value" + index;
                Map<String, Object> result = (Map<String, Object>) JToon.decode(input);
                assertEquals("value" + index, result.get("key"));
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
    }
}
