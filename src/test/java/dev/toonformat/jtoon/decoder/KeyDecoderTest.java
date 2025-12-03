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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class KeyDecoderTest {

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<KeyDecoder> constructor = KeyDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    @DisplayName("Given SAFE path expansion and valid dotted key When checked Then key is expandable")
    void shouldExpandKey_givenSafeAndValidDotted_whenChecked_thenTrue() {
        // Given
        DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE);

        // When
        boolean expandable = KeyDecoder.shouldExpandKey("user.name", context);

        // Then
        assertTrue(expandable);
    }

    @Test
    @DisplayName("Given SAFE path expansion and valid dotted key When checked Then key is expandable")
    void shouldExpandKeyGivenKeyWithQutesWhenCheckedThenTrue() {
        // Given
        DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.SAFE);

        // When
        boolean expandable = KeyDecoder.shouldExpandKey("\"user.name\"", context);

        // Then
        assertFalse(expandable);
    }

    @Test
    @DisplayName("Given OFF path expansion When checked Then dotted key is not expandable")
    void shouldExpandKey_givenOff_whenChecked_thenFalse() {
        // Given
        DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);

        // When
        boolean expandable = KeyDecoder.shouldExpandKey("user.name", context);

        // Then
        assertFalse(expandable);
    }

    @Test
    @DisplayName("Given quoted key When checked Then key is not expandable")
    void shouldExpandKey_givenQuoted_whenChecked_thenFalse() {
        // Given
        DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE);

        // When
        boolean expandable = KeyDecoder.shouldExpandKey("\"user.name\"", context);

        // Then
        assertFalse(expandable);
    }

    @Test
    @DisplayName("Given empty target map and dotted key When expanded Then nested structure is created")
    void expandPathIntoMap_givenDottedKey_whenExpanded_thenCreatesNested() {
        // Given
        Map<String, Object> target = new LinkedHashMap<>();
        DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE);

        // When
        KeyDecoder.expandPathIntoMap(target, "a.b.c", 1, context);

        // Then
        assertTrue(target.containsKey("a"));
        Object a = target.get("a");
        assertInstanceOf(Map.class, a);
        @SuppressWarnings("unchecked") Map<String, Object> aMap = (Map<String, Object>) a;
        Object b = aMap.get("b");
        assertInstanceOf(Map.class, b);
        @SuppressWarnings("unchecked") Map<String, Object> bMap = (Map<String, Object>) b;
        assertEquals(1, bMap.get("c"));
    }

    @Test
    @DisplayName("Given dotted key/value line and SAFE expansion When processed Then value is placed in nested map")
    void processKeyValueLine_givenDottedKey_whenProcessed_thenNestedMapContainsValue() {
        // Given
        Map<String, Object> result = new LinkedHashMap<>();
        DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE);
        context.lines = new String[] { "user.name: Ada" };
        context.currentLine = 0;

        // When
        KeyDecoder.processKeyValueLine(result, context.lines[0], 0, context);

        // Then
        assertTrue(result.containsKey("user"));
        @SuppressWarnings("unchecked") Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertEquals("Ada", user.get("name"));
    }

    @Test
    @DisplayName("Given wrong content When processed Then value is placed in nested map")
    void processKeyValueLine_givenWrongContent() {
        // Given
        Map<String, Object> result = new LinkedHashMap<>();
        DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.SAFE);
        context.lines = new String[] { "invalid line" };
        context.currentLine = 0;

        // When
        KeyDecoder.processKeyValueLine(result, context.lines[0], 0, context);

        // Then
        assertEquals(1, context.currentLine);
    }

    @Test
    @DisplayName("Given invalid key/value line in strict mode When processed Then exception is thrown")
    void processKeyValueLine_givenMissingColonStrict_whenProcessed_thenThrows() {
        // Given
        Map<String, Object> result = new LinkedHashMap<>();
        DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE);
        context.lines = new String[] { "invalid line" };
        context.currentLine = 0;

        // When / Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                                   () -> KeyDecoder.processKeyValueLine(result, context.lines[0], 0, context));
        assertTrue(ex.getMessage().contains("Missing colon"));
    }

    @Test
    @DisplayName("Given key-value pair with dotted key When parsed Then resulting object has nested structure")
    void parseKeyValuePair_givenDottedKey_whenParsed_thenNestedStructure() {
        // Given
        DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE);
        context.lines = new String[] { "a.b: 1" };
        context.currentLine = 0;

        // When
        Object obj = KeyDecoder.parseKeyValuePair("a.b", "1", 0, false, context);

        // Then
        assertInstanceOf(Map.class, obj);
        @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) obj;
        @SuppressWarnings("unchecked") Map<String, Object> a = (Map<String, Object>) map.get("a");
        assertEquals(1L, a.get("b"));
    }

    @Test
    @DisplayName("No Quted key When checked Then key is expandable")
    void parseKeyValueField_NoQuotes_givenSafeAndValidDotted_whenChecked_thenTrue() {
        // Given
        DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.SAFE);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user.name", "Ada");

        int depth = 0;

        // When
        boolean expandable = KeyDecoder.parseKeyValueField("\"user.name\"", map, depth, context);

        // Then
        assertFalse(expandable);
    }
}
