package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.PathExpansion;
import dev.toonformat.jtoon.util.Headers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class KeyDecoderTest {

    private static final int TEST_PATH_VALUE = 123;

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<KeyDecoder> constructor = KeyDecoder.class.getDeclaredConstructor();
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
    @DisplayName("Given SAFE path expansion and valid dotted key When checked Then key is expandable")
    void shouldExpandKey_givenSafeAndValidDotted_whenChecked_thenTrue() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

        // When
        final boolean expandable = KeyDecoder.shouldExpandKey("user.name", context);

        // Then
        assertTrue(expandable);
    }

    @Test
    @DisplayName("Given SAFE path expansion and valid dotted key When checked Then key is expandable")
    void shouldExpandKeyGivenKeyWithQutesWhenCheckedThenTrue() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

        // When
        final boolean expandable = KeyDecoder.shouldExpandKey("\"user.name\"", context);

        // Then
        assertFalse(expandable);
    }

    @Test
    @DisplayName("Given OFF path expansion When checked Then dotted key is not expandable")
    void shouldExpandKey_givenOff_whenChecked_thenFalse() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

        // When
        final boolean expandable = KeyDecoder.shouldExpandKey("user.name", context);

        // Then
        assertFalse(expandable);
    }

    @Test
    @DisplayName("Given quoted key When checked Then key is not expandable")
    void shouldExpandKey_givenQuoted_whenChecked_thenFalse() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

        // When
        final boolean expandable = KeyDecoder.shouldExpandKey("\"user.name\"", context);

        // Then
        assertFalse(expandable);
    }

    @Test
    @DisplayName("Given empty target map and dotted key When expanded Then nested structure is created")
    void expandPathIntoMap_givenDottedKey_whenExpanded_thenCreatesNested() {
        // Given
        final Map<String, Object> target = new LinkedHashMap<>();
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

        // When
        KeyDecoder.expandPathIntoMap(target, "a.b.c", 1, context);

        // Then
        assertTrue(target.containsKey("a"));
        final Object a = target.get("a");
        assertInstanceOf(Map.class, a);
        @SuppressWarnings("unchecked") final Map<String, Object> aMap = (Map<String, Object>) a;
        final Object b = aMap.get("b");
        assertInstanceOf(Map.class, b);
        @SuppressWarnings("unchecked") final Map<String, Object> bMap = (Map<String, Object>) b;
        assertEquals(1, bMap.get("c"));
    }

    @Test
    void testThrowsIllegalArgumentExceptionWhenPathConflictsInStrictMode() {
        // Given
        final Map<String, Object> root = new LinkedHashMap<>();

        root.put("foo", "notAMap");

        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);


        // When / Then
        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> KeyDecoder.expandPathIntoMap(root, "foo.bar.baz", TEST_PATH_VALUE, context)
        );

        assertTrue(ex.getMessage().contains("Path expansion conflict"));
        assertTrue(ex.getMessage().contains("foo"));
    }

    @Test
    void testCallsExpandPathIntoMapWhenShouldExpandKeyTrue() {
        // Given
        final Map<String, Object> result = new LinkedHashMap<>();
                final String content = "foo.bar[#0]:";
        final int parentDepth = 0;

        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.lines = new String[]{content};

        final List<Object> expectedArray = Arrays.asList(1, 2, 3);

        // When
        KeyDecoder.processKeyedArrayLine(result, content, Headers.matchKeyedArrayHeader(content), parentDepth, context);

        // Then
        final Map<String, Object> expectedNestedMap = new LinkedHashMap<>();
        expectedNestedMap.put("bar", expectedArray);
        assertNull(result.get("bar"));
        assertEquals(result.size(),  expectedNestedMap.size());
    }

    @Test
    @DisplayName("Given basic keyed array line When processed Then value is placed in map")
    void processKeyedArrayLine_givenBasicKeyedArray_whenProcessed_thenValueInMap() {
        // Given
        final Map<String, Object> result = new LinkedHashMap<>();
        final String content = "tags[3]: a, b, c";
                final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.delimiter = Delimiter.COMMA;

        // When
        KeyDecoder.processKeyedArrayLine(result, content, Headers.matchKeyedArrayHeader(content), 0, context);

        // Then
        final List<Object> expected = Arrays.asList("a", "b", "c");
        assertEquals(expected, result.get("tags"));
    }

    @Test
    @DisplayName("Given dotted keyed array line and SAFE expansion When processed Then value is placed in nested map")
    void processKeyedArrayLine_givenDottedKeyedArray_whenProcessed_thenNestedMapContainsValue() {
        // Given
        final Map<String, Object> result = new LinkedHashMap<>();
        final String content = "user.tags[2]: dev, test";
                final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.delimiter = Delimiter.COMMA;

        // When
        KeyDecoder.processKeyedArrayLine(result, content, Headers.matchKeyedArrayHeader(content), 0, context);

        // Then
        assertTrue(result.containsKey("user"));
        @SuppressWarnings("unchecked") final Map<String, Object> user = (Map<String, Object>) result.get("user");
        final List<Object> expected = Arrays.asList("dev", "test");
        assertEquals(expected, user.get("tags"));
    }

    @Test
    @DisplayName("Given dotted keyed array line and expansion conflict in strict mode"
            + " When processed Then throws exception")
    void processKeyedArrayLine_givenExpansionConflictStrict_whenProcessed_thenThrowsException() {
        // Given
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", "not-a-map");
        final String content = "user.tags[1]: dev";
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.delimiter = Delimiter.COMMA;

        // When / Then
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> KeyDecoder.processKeyedArrayLine(result, content, Headers.matchKeyedArrayHeader(content), 0, context));
        assertTrue(ex.getMessage().contains("Path expansion conflict"));
    }

    @Test
    void testEmptyValueCreatesLinkedHashMap() throws Exception {
        // Given
        final String value = " ";
        final int depth = 25;

        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.lines = new String[]{"key:   "};
        context.currentLine = -1;

        // When
        final Object result = invokePrivateStatic("parseKeyValue",
                new Class[]{String.class, int.class, DecodeContext.class}, value, depth, context);

        // Then
        assertInstanceOf(LinkedHashMap.class, result, "Expected a LinkedHashMap for empty value");
        assertTrue(((Map<?, ?>) result).isEmpty(), "LinkedHashMap should be empty");
        assertEquals(0, context.currentLine);
    }

    @Test
    void testExpandPathIntoMapCalledWhenShouldExpandKeyTrue() {
        // Given
        final Map<String, Object> item = new LinkedHashMap<>();
        final String fieldContent = "foo.bar: someValue"; // contains colon
        final int depth = 0;

        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.lines = fieldContent.split(" ");
        final Object parsedValue = "someValue";

        // When
        final boolean result = KeyDecoder.parseKeyValueField(fieldContent, item, depth, context);

        // Then
        assertTrue(result, "Should return true for field with colon");

        final Map<String, Object> expectedNestedMap = new LinkedHashMap<>();
        expectedNestedMap.put("bar", parsedValue);
        assertEquals(expectedNestedMap, item.get("foo"));
    }


    @Test
    @DisplayName("Given dotted key/value line and SAFE expansion When processed Then value is placed in nested map")
    void processKeyValueLine_givenDottedKey_whenProcessed_thenNestedMapContainsValue() {
        // Given
        final Map<String, Object> result = new LinkedHashMap<>();
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.lines = new String[]{"user.name: Ada"};
        context.currentLine = 0;

        // When
        KeyDecoder.processKeyValueLine(result, context.lines[0], 0, context);

        // Then
        assertTrue(result.containsKey("user"));
        @SuppressWarnings("unchecked") final Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertEquals("Ada", user.get("name"));
    }

    @Test
    @DisplayName("Given wrong content When processed Then value is placed in nested map")
    void processKeyValueLine_givenWrongContent() {
        // Given
        final Map<String, Object> result = new LinkedHashMap<>();
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.lines = new String[]{"invalid line"};
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
        final Map<String, Object> result = new LinkedHashMap<>();
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.lines = new String[]{"invalid line"};
        context.currentLine = 0;

        // When / Then
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> KeyDecoder.processKeyValueLine(result, context.lines[0], 0, context));
        assertTrue(ex.getMessage().contains("Missing colon"));
    }

    @Test
    @DisplayName("Given key-value pair with dotted key When parsed Then resulting object has nested structure")
    void parseKeyValuePair_givenDottedKey_whenParsed_thenNestedStructure() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.lines = new String[]{"a.b: 1"};
        context.currentLine = 0;

        // When
        final Object obj = KeyDecoder.parseKeyValuePair("a.b", "1", 0, false, context);

        // Then
        assertInstanceOf(Map.class, obj);
        @SuppressWarnings("unchecked") final Map<String, Object> map = (Map<String, Object>) obj;
        @SuppressWarnings("unchecked") final Map<String, Object> a = (Map<String, Object>) map.get("a");
        assertEquals(1L, a.get("b"));
    }

    @Test
    @DisplayName("No Quted key When checked Then key is expandable")
    void parseKeyValueField_NoQuotes_givenSafeAndValidDotted_whenChecked_thenTrue() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.SAFE,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("user.name", "Ada");

        final int depth = 0;

        // When
        final boolean expandable = KeyDecoder.parseKeyValueField("\"user.name\"", map, depth, context);

        // Then
        assertFalse(expandable);
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(final String methodName, final Class<?>[] paramTypes,
            final Object... args) throws Exception {
        final Method declaredMethod = KeyDecoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }
}
