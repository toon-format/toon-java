package dev.toonformat.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


@Tag("unit")
public class JToonSpecialNumbersTest {

    @Test
    @DisplayName("NaN should encode to null")
    void testEncodeNaN() {

        String toon = JToon.encode(Double.NaN);
        assertNotNull(toon, "Encoding result must not be null");
        assertTrue(toon.contains("null"), "NaN should encode to null");
    }


    @Test
    @DisplayName("Positive Infinity should encode to null")
    void testEncodePositiveInfinity() {

        String toon = JToon.encode(Double.POSITIVE_INFINITY);
        assertNotNull(toon);
        assertTrue(toon.contains("null"), "Positive Infinity should encode to null");
    }


    @Test
    @DisplayName("Negative Infinity should encode to null")
    void testEncodeNegativeInfinity() {

        String toon = JToon.encode(Double.NEGATIVE_INFINITY);
        assertNotNull(toon);
        assertTrue(toon.contains("null"), "Negative Infinity should encode to null");
    }
}
