package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValueDecoder utility class.
 * Tests decoding of primitive values, keys, and header formatting.
 */
@Tag("unit")
class ValueDecoderTest {

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<ValueDecoder> constructor = ValueDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
                assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    @DisplayName("parses list items whose first field is a tabular array")
    void decodeTabularArray() {
        // Given
        String input = "items[1]:\n  - users[2]{id,name}:\n      1,Ada\n      2,Bob\n    status: active";

        // When
        String result = ValueDecoder.decodeToJson(input, DecodeOptions.DEFAULT);

        // Then
        assertEquals("{\"items\":[{\"users\":[{\"id\":1,\"name\":\"Ada\"},{\"id\":2,\"name\":\"Bob\"}],\"status\":\"active\"}]}", result);
    }

    @Test
    @DisplayName("parses arrays of arrays within objects")
    void decodeArraysOfArraysWithinObjects() {
        // Given
        String input = "items[1]:\n  - matrix[2]:\n      - [2]: 1,2\n      - [2]: 3,4\n    name: grid";

        // When
        String result = ValueDecoder.decodeToJson(input, DecodeOptions.DEFAULT);

        // Then
        assertEquals("{\"items\":[{\"matrix\":[[1,2],[3,4]],\"name\":\"grid\"}]}", result);
    }
}
