package dev.toonformat.jtoon;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

class JToonConcurrencyTest {

    private static final int AWAIT_TIMEOUT_SECONDS = 10;

    @Test
    @SuppressWarnings("FutureReturnValueIgnored")
    void encodeDecodeStressTest() {
        final int threads = 8;
        final int tasksPerThread = 5_000;

        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch latch = new CountDownLatch(threads * tasksPerThread);
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        final Runnable task = () -> {
            try {
                // Given
                final Map<String, Object> data = new LinkedHashMap<>();
                data.put("id", ThreadLocalRandom.current().nextInt());
                data.put("name", "Alice");
                data.put("tags", List.of("x", "y", "z"));

                // When
                final String toon = JToon.encode(data);

                // Then
                assertNotNull(toon);
                final Object decoded = JToon.decode(toon);
                assertNotNull(decoded);

            } catch (Throwable ex) {
                errors.add(ex);
            } finally {
                latch.countDown();
            }
        };

        for (int i = 0; i < threads * tasksPerThread; i++) {
            executor.submit(task);
        }

        await()
            .atMost(AWAIT_TIMEOUT_SECONDS, SECONDS)
            .until(() -> latch.getCount() == 0);

        executor.shutdown();

        assertTrue(errors.isEmpty(), "Errors occurred in threads: " + errors);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void encodeDecodeJsonStressTest() {
        final int threads = 8;
        final int tasksPerThread = 5_000;

        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch latch = new CountDownLatch(threads * tasksPerThread);
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        final Runnable task = () -> {
            try {
                // Given
                final String json = "{\"foo\":123, \"bar\":[\"a\",\"b\"]}";

                // When
                final String toon = JToon.encodeJson(json);

                // Then
                assertNotNull(toon);
                final String roundTrip = JToon.decodeToJson(toon);
                assertNotNull(roundTrip);
                assertTrue(roundTrip.contains("\"foo\":123"));

            } catch (Throwable ex) {
                errors.add(ex);
            } finally {
                latch.countDown();
            }
        };

        for (int i = 0; i < threads * tasksPerThread; i++) {
            executor.submit(task);
        }

        await()
            .atMost(AWAIT_TIMEOUT_SECONDS, SECONDS)
            .until(() -> latch.getCount() == 0);

        executor.shutdown();

        assertTrue(errors.isEmpty(), "Errors occurred in threads: " + errors);
    }

}
