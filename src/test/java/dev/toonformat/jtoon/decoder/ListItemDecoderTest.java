package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class ListItemDecoderTest {

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<ListItemDecoder> constructor = ListItemDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = ListItemDecoder.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    @Test
    @DisplayName("Process list array item, with random string")
    void testProcessListArrayItem() {
        // Given
        String line = "sadasdasdasd";
        int lineDepth = 2;
        int depth = 1;
        List<Object> result = List.of();
        DecodeContext context = new DecodeContext();
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
        String line = "sadasdasdasd";
        int lineDepth = 1;
        int depth = 3;
        List<Object> result = List.of();
        DecodeContext context = new DecodeContext();
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
        String line = "  - asd";
        Object testObject = new Object();
        Map<String, Object> item = Map.of(line, testObject);
        int depth = -2;
        DecodeContext context = new DecodeContext();
        context.options = DecodeOptions.DEFAULT;
        context.lines = new String[] { line };

        // When
        invokePrivateStatic("parseListItemFields", new Class[] { Map.class, int.class, DecodeContext.class }, item, depth, context);

        // Then
        assertEquals(1, context.currentLine);
    }
}
