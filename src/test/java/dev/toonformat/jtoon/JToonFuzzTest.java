package dev.toonformat.jtoon;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class JToonFuzzTest {

    private static final SplittableRandom RANDOM = new SplittableRandom();

    @Test
    @Tag("fuzz")
    void fuzzUnicodeInput() {
        final String[] evil = {
            "\u0000",       // null char
            "\uD800",       // broken surrogate
            "\uFFFF",
            "\u2028",       // line separator
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
        for (int i = 0; i < 1_000; i++) {
            byte[] bytes = new byte[RANDOM.nextInt(500)];
            RANDOM.nextBytes(bytes);
            String input = new String(bytes);

            assertTimeoutPreemptively(
                Duration.ofMillis(100),
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
