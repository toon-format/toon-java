package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import dev.toonformat.jtoon.DecodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ListItemDecoderTest {

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
}
