package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.PathExpansion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @Test
    void decode_returnsEmptyMap_whenProcessedIsEmpty() {
        // Given
        String input = "   ";  // only whitespace

        // When
        DecodeOptions options = new DecodeOptions();
        Object result = ValueDecoder.decode(input, options);

        // Then
        assertInstanceOf(LinkedHashMap.class, result, "Result must be an empty LinkedHashMap");
        assertTrue(((LinkedHashMap<?, ?>) result).isEmpty(), "Map must be empty");
    }

    @Test
    @DisplayName("Should parse TOON format primitive array to JSON")
    void parsePrimitiveArray() {
        // When
        Object parseValue = ValueDecoder.decode("items[3]: a,\"b,c\",\"d:e\"", DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{items=[a, b,c, d:e]}", parseValue.toString());
    }

    @Test
    @DisplayName("Should parse TOON format tabular array to JSON")
    void parseTabularArray() {
        // When
        Object parseValue = ValueDecoder.decode("items[2]{id,name}:\n  1,Alice\n  2,Bob\ncount: 2", DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{items=[{id=1, name=Alice}, {id=2, name=Bob}], count=2}", parseValue.toString());
    }

    @Test
    @DisplayName("Should parse TOON format nested array to JSON")
    void parseNestedArray() {
        // When
        Object parseValue = ValueDecoder.decode(
            "items[1]:\n  - users[2]{id,name}:\n      1,Ada\n      2,Bob\n    status: active"
            , DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{items=[{users=[{id=1, name=Ada}, {id=2, name=Bob}], status=active}]}", parseValue.toString());
    }

    @Test
    @DisplayName("Should parse TOON format object to JSON")
    void parseObject() {
        // When
        Object parseValue = ValueDecoder.decode("id: 123\nname: Ada\nactive: true", DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{id=123, name=Ada, active=true}", parseValue.toString());
    }

    @Test
    @DisplayName("Should parse TOON format number to JSON")
    void parseNumber() {
        // When
        Object parseValue = ValueDecoder.decode("value: 1.5000", DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{value=1.5}", parseValue.toString());
    }

    @Test
    @DisplayName("Should parse TOON format to JSON tolerating whitespaces")
    void parseToleratingSpacesInCommas() {
        Object parseValue = ValueDecoder.decode("tags[3]: a , b , c", DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{tags=[a, b, c]}", parseValue.toString());
    }

    @Test
    void givenNoLines_whenParse_thenReturnEmptyMap() {
        // Given
        DecodeOptions decodeOptions = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF);
        Object parseValue = ValueDecoder.decode("  indented", decodeOptions);// depth=1

        // Then
        assertNotNull(parseValue);
        assertInstanceOf(Map.class, parseValue);
    }

    @Test
    void givenIndentedLineAndStrict_whenParse_thenThrow() {
        // Given
        DecodeOptions decodeOptions = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> ValueDecoder.decode("  indented", decodeOptions));
    }

    @Test
    void decode_keyValuePair_callsKeyDecoder() {
        DecodeOptions decodeOptions = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF);

        Object result = ValueDecoder.decode("name: Ada", decodeOptions);

        // Whatever KeyDecoder returns, you simply assert expected behavior.
        // Usually: { "name" : "Ada" } as a map
        assertInstanceOf(Map.class, result);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.size());
        assertEquals("Ada", map.get("name"));
    }


    @Test
    void decodeToJson_throwsWrappedException_whenDecodeFails() {
        DecodeOptions options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);

        String invalidIndentedInput = "  badIndent";

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ValueDecoder.decodeToJson(invalidIndentedInput, options)
        );

        assertTrue(ex.getMessage().contains("Failed to convert decoded value to JSON"));
        assertNotNull(ex.getCause());  // original decode() exception is preserved
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Unexpected indentation"));
    }

}
