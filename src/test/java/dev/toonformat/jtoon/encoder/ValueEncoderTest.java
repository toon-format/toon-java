package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class ValueEncoderTest {

    private final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<ValueEncoder> constructor = ValueEncoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When
        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        // Then
        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    @DisplayName("given primitive JsonNode when encodeValue then returns encoded primitive")
    void givenPrimitive_whenEncodeValue_thenReturnsEncodedPrimitive() {
        // Given
        var number = jsonNodeFactory.numberNode(42);
        var options = EncodeOptions.DEFAULT;

        // When
        String result = ValueEncoder.encodeValue(number, options);

        // Then
        assertEquals("42", result);
    }

    @Test
    @DisplayName("given primitive array when encodeValue then writes inline array header and values")
    void givenPrimitiveArray_whenEncodeValue_thenWritesInlineArray() {
        // Given
        ArrayNode array = jsonNodeFactory.arrayNode().add(1).add(2).add(3);
        var options = EncodeOptions.DEFAULT;

        // When
        String result = ValueEncoder.encodeValue(array, options);

        // Then
        assertEquals("[3]: 1,2,3", result);
    }

    @Test
    @DisplayName("given simple object when encodeValue then writes key-value lines")
    void givenObject_whenEncodeValue_thenWritesObjectLines() {
        // Given
        ObjectNode obj = jsonNodeFactory.objectNode();
        obj.put("a", 1);
        obj.put("b", "x");
        var options = EncodeOptions.DEFAULT;

        // When
        String result = ValueEncoder.encodeValue(obj, options);

        // Then
        String expected = String.join("\n",
                "a: 1",
                "b: x");
        assertEquals(expected, result);
    }
}
