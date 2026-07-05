package dev.toonformat.jtoon;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class JToonFuzzTest {

    private static final SplittableRandom RANDOM = new SplittableRandom();
    private static final int FUZZ_ITERATIONS = 1_000;
    private static final int MAX_FUZZ_BYTE_LENGTH = 500;
    private static final long FUZZ_TIMEOUT_MILLIS = 200;

    @Test
    @Tag("fuzz")
    void fuzzUnicodeInput() {
        final String[] evil = {
            "\0",
            String.valueOf((char) 0xD800),
            String.valueOf((char) 0xFFFF),
            String.valueOf((char) 0x2028),
            "💣",           // emoji
            "漢字"
        };
        assertDoesNotThrow(() -> Arrays.stream(evil).forEach(s -> {
            try {
                JToon.decode("{\"x\":\"" + s + "\"}");
            } catch (RuntimeException e) {
                // acceptable
            }
        }));
    }


    @Test
    @Tag("fuzz")
    void fuzzDoesNotHang() {
        for (int i = 0; i < FUZZ_ITERATIONS; i++) {
            final byte[] bytes = new byte[RANDOM.nextInt(MAX_FUZZ_BYTE_LENGTH)];
            RANDOM.nextBytes(bytes);
            final String input = new String(bytes, StandardCharsets.UTF_8);

            assertTimeoutPreemptively(
                Duration.ofMillis(FUZZ_TIMEOUT_MILLIS),
                () -> {
                    try {
                        JToon.decode(input);
                    } catch (RuntimeException e) {
                        // expected
                    }
                }
            );
        }
    }


}
