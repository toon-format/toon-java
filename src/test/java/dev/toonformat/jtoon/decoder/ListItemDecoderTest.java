package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.toonformat.jtoon.DecodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ListItemDecoderTest {

    private static final long SCALAR_ITEM_VALUE = 42L;

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<ListItemDecoder> constructor = ListItemDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When
        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        // Then
        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(final String methodName, final Class<?>[] paramTypes,
            final Object... args) throws Exception {
        final Method declaredMethod = ListItemDecoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }

    @Test
    @DisplayName("Process list array item, with random string")
    void testProcessListArrayItem() {
        // Given
        final String line = "sadasdasdasd";
        final int lineDepth = 2;
        final int depth = 1;
        final List<Object> result = List.of();
        final DecodeContext context = new DecodeContext();
        context.options = DecodeOptions.DEFAULT;

        // When
        ListItemDecoder.processListArrayItem(line, lineDepth, depth, result, context);

        // Then
        assertEquals(1, context.currentLine);
    }

    @Test
    @DisplayName("Process list array item, with a to small line depth")
    void testProcessListArrayItemWithTooSmallLineDepth() {
        // Given
        final String line = "sadasdasdasd";
        final int lineDepth = 1;
        final int depth = 3;
        final List<Object> result = List.of();
        final DecodeContext context = new DecodeContext();
        context.options = DecodeOptions.DEFAULT;

        // When
        ListItemDecoder.processListArrayItem(line, lineDepth, depth, result, context);

        // Then
        assertEquals(1, context.currentLine);
    }

    @Test
    @DisplayName("Testing parseListItemFields with negativ depth")
    void testParseListItemFields() throws Exception {
        // Given
        final String line = "  - asd";
        final Object testObject = new Object();
        final Map<String, Object> item = Map.of(line, testObject);
        final int depth = -2;
        final DecodeContext context = new DecodeContext();
        context.options = DecodeOptions.withStrict(false);
        context.lines = new String[] { line };

        // When
        invokePrivateStatic("parseListItemFields",
                new Class[] { Map.class, int.class, DecodeContext.class }, item, depth, context);

        // Then
        assertEquals(1, context.currentLine);
    }

    @Test
    @DisplayName("Given scalar item When parsed Then scalar returned and line advanced")
    void parseListItem_givenScalarItem_whenParsed_thenScalar() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"- 42"};
        context.currentLine = 0;

        // When
        final Object result = ListItemDecoder.parseListItem("- 42", 0, context);

        // Then
        assertEquals(SCALAR_ITEM_VALUE, result);
        assertEquals(1, context.currentLine);
    }

    @Test
    @DisplayName("Given empty item When parsed Then empty map returned")
    void parseListItem_givenEmptyItem_whenParsed_thenEmptyMap() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"- "};
        context.currentLine = 0;

        // When
        final Object result = ListItemDecoder.parseListItem("- ", 0, context);

        // Then
        assertInstanceOf(Map.class, result);
        assertTrue(((Map<?, ?>) result).isEmpty());
        assertEquals(1, context.currentLine);
    }

    @Test
    @DisplayName("Given standalone array item When parsed Then list returned")
    void parseListItem_givenStandaloneArray_whenParsed_thenList() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"- [2]: 1,2"};
        context.currentLine = 0;

        // When
        final Object result = ListItemDecoder.parseListItem("- [2]: 1,2", 0, context);

        // Then
        assertEquals("[1, 2]", result.toString());
        assertEquals(1, context.currentLine);
    }

    @Test
    @DisplayName("Given keyless fields header item in strict mode When parsed Then exception")
    void parseListItem_givenKeylessFieldsHeaderStrict_whenParsed_thenThrows() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"- [2]{x}: 1"};
        context.currentLine = 0;

        // When / Then
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> ListItemDecoder.parseListItem("- [2]{x}: 1", 0, context));
        assertTrue(ex.getMessage().contains("Keyless array header with field list"));
    }

    @Test
    @DisplayName("Given keyed array item When parsed Then map with list returned")
    void parseListItem_givenKeyedArray_whenParsed_thenMapWithList() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"- tags[2]: a,b"};
        context.currentLine = 0;

        // When
        final Object result = ListItemDecoder.parseListItem("- tags[2]: a,b", 0, context);

        // Then
        assertInstanceOf(Map.class, result);
        assertEquals("[a, b]", ((Map<?, ?>) result).get("tags").toString());
        assertEquals(1, context.currentLine);
    }

    @Test
    @DisplayName("Given object item When parsed Then map returned")
    void parseListItem_givenObjectItem_whenParsed_thenMap() {
        // Given
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"- key: value"};
        context.currentLine = 0;

        // When
        final Object result = ListItemDecoder.parseListItem("- key: value", 0, context);

        // Then
        assertInstanceOf(Map.class, result);
        assertEquals("value", ((Map<?, ?>) result).get("key"));
        assertEquals(1, context.currentLine);
    }

    @Test
    @DisplayName("Given over-indented field line in strict mode When parsed Then exception")
    void parseListItemFields_givenOverIndentedStrict_whenParsed_thenThrows() throws Exception {
        // Given
        final Map<String, Object> item = new LinkedHashMap<>();
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"  - item", "      orphan", "  - next"};
        context.currentLine = 1;

        // When / Then
        final InvocationTargetException ex = assertThrows(InvocationTargetException.class,
            () -> invokePrivateStatic("parseListItemFields",
                new Class[]{Map.class, int.class, DecodeContext.class}, item, 0, context));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    @DisplayName("Given over-indented field line in lenient mode When parsed Then line skipped")
    void parseListItemFields_givenOverIndentedLenient_whenParsed_thenSkipped() throws Exception {
        // Given
        final Map<String, Object> item = new LinkedHashMap<>();
        final DecodeContext context = new DecodeContext();
        context.options = DecodeOptions.withStrict(false);
        context.lines = new String[]{"  - item", "      orphan", "  - next"};
        context.currentLine = 1;

        // When
        invokePrivateStatic("parseListItemFields",
            new Class[]{Map.class, int.class, DecodeContext.class}, item, 0, context);

        // Then
        assertEquals(2, context.currentLine);
    }

    @Test
    @DisplayName("Given sibling field lines When parsed Then fields added to item")
    void parseListItemFields_givenFieldLines_whenParsed_thenFieldsAdded() throws Exception {
        // Given
        final Map<String, Object> item = new LinkedHashMap<>();
        final DecodeContext context = new DecodeContext();
        context.lines = new String[]{"  - item", "    extra: 1", "  - next"};
        context.currentLine = 1;

        // When
        invokePrivateStatic("parseListItemFields",
            new Class[]{Map.class, int.class, DecodeContext.class}, item, 0, context);

        // Then
        assertEquals(1L, item.get("extra"));
        assertEquals(2, context.currentLine);
    }
}
