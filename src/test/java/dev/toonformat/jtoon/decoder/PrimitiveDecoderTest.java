package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.encoder.PrimitiveEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PrimitiveDecoder utility class.
 * Tests decoding of primitive values, keys, and header formatting.
 */
@Tag("unit")
class PrimitiveDecoderTest {

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<PrimitiveEncoder> constructor = PrimitiveEncoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    void givenNullInput_whenParse_thenReturnsEmptyString() {
        // Given
        String input = null;

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    void givenEmptyInput_whenParse_thenReturnsEmptyString() {
        // Given
        String input = "";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals("", result);
    }

    @Test
    void givenNullLiteral_whenParse_thenReturnsNull() {
        // Given
        String input = "null";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNull(result);
    }

    @Test
    void givenTrueLiteral_whenParse_thenReturnsBooleanTrue() {
        // Given
        String input = "true";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(true, result);
    }

    @Test
    void givenFalseLiteral_whenParse_thenReturnsBooleanFalse() {
        // Given
        String input = "false";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(false, result);
    }

    @Test
    void givenQuotedString_whenParse_thenReturnsUnescapedString() {
        // Given
        String input = "\"hello\\nworld\"";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals("hello\nworld", result);

    }

    @Test
    void givenOctalLikeNumber_whenParse_thenReturnsString() {
        // Given
        String input = "0123"; // starts with "0" + non-decimal

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals("0123", result);
    }

    @Test
    void givenZeroOrExplicitZeroFormats_whenParse_thenParsesAsNumber() {
        // Given
        String input = "0.0";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(0L, result);  // negative/positive zero → 0L
    }

    @Test
    void givenIntegerString_whenParse_thenReturnsLong() {
        // Given
        String input = "42";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(42L, result);
    }

    @Test
    void givenDecimalNumber_whenParse_thenReturnsDouble() {
        // Given
        String input = "3.14";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals(3.14, (Double) result, 0.000001);
    }

    @Test
    void givenExponentNumber_whenParse_thenReturnsLong() {
        // Given
        String input = "1e3";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals(1000.0, (Long) result, 0.000001);
    }

    @Test
    void givenDoubleRepresentingWholeNumber_whenParse_thenReturnsLong() {
        // Given
        String input = "42.0";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(42L, result); // should convert to Long
    }

    @Test
    void givenNegativeZeroDouble_whenParse_thenReturnsZeroLong() {
        // Given
        String input = "-0.0";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals(0L, result);
    }

    @Test
    void givenOctalNumber_whenParse_thenReturnsLong() {
        // Given
        String input = "07";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("07", result.toString());
    }

    @Test
    void givenNumberWithLeadingZero_whenParse_thenReturnsLong() {
        // Given
        String input = "0.7";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("0.7", result.toString());
    }

    @Test
    void givenNumberWithLeadingZeroOutsideTheOctalRange_whenParse_thenReturnsLong() {
        // Given
        String input = "0.9";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("0.9", result.toString());
    }

    @Test
    void givenMinLongNumber_whenParse_thenReturnsLong() {
        // Given
        String input = String.valueOf(Long.MIN_VALUE);

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("-9223372036854775808", result.toString());
    }

    @Test
    void givenMaxLongNumber_whenParse_thenReturnsLong() {
        // Given
        String input = String.valueOf(Long.MAX_VALUE);

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("9223372036854775807", result.toString());
    }

    @Test
    void givenSmallerMinLongNumber_whenParse_thenReturnsLong() {
        // Given
        String input = String.valueOf(Long.MIN_VALUE - 1);

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("9223372036854775807", result.toString());
    }

    @Test
    void givenBiggerMaxLongNumber_whenParse_thenReturnsLong() {
        // Given
        String input = String.valueOf(Long.MAX_VALUE + 1);

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertNotNull(result);
        assertEquals("-9223372036854775808", result.toString());
    }


    @Test
    void givenInvalidNumber_whenParse_thenReturnsOriginalString() {
        // Given
        String input = "123abc";

        // When
        Object result = PrimitiveDecoder.parse(input);

        // Then
        assertEquals("123abc", result);
    }

    @Test
    void testing_SkipTrailingZeros() throws Exception {
        // Given
        String input = "10.000";

        // When
        String result = (String) invokePrivateStatic("stripTrailingZeros", new Class[]{String.class}, input);

        // Then
        assertEquals("10", result);
    }

    @Test
    void testing_SkipTrailingZeros_WithSmallNUmber() throws Exception {
        // Given
        String input = "1.0";

        // When
        String result = (String) invokePrivateStatic("stripTrailingZeros", new Class[]{String.class}, input);

        // Then
        assertEquals("1", result);
    }



    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method declaredMethod = PrimitiveEncoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }
}
