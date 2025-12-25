package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ArrayDecoderTest {

    private final DecodeContext context = new DecodeContext();

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<ArrayDecoder> constructor = ArrayDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method declaredMethod = ArrayDecoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }

    @Test
    @DisplayName("Should parse TOON format numerical array to JSON")
    void parseNumericalPrimitiveArray() {
        setUpContext("[3]: 1,2,3");
        List<Object> result = ArrayDecoder.parseArray("[3]: 1,2,3", 0, context);
        assertEquals("[1, 2, 3]", result.toString());
    }

    @Test
    @DisplayName("Should parse TOON format string array to JSON")
    void parseStrPrimitiveArray() {
        setUpContext("[3]: reading,gaming,coding");
        List<Object> result = ArrayDecoder.parseArray("[3]: reading,gaming,coding", 0, context);
        assertEquals("[reading, gaming, coding]", result.toString());
    }

    @Test
    @DisplayName("Should parse TOON format tabular array to JSON")
    void parseTabularArray() {
        setUpContext("[2]{sku,qty,price}:\n  A1,2,9.99\n  B2,1,14.5");
        List<Object> result = ArrayDecoder.parseArray("[2]{sku,qty,price}:\n  A1,2,9.99\n  B2,1,14.5", 0, context);
        assertEquals("[{sku=A1, qty=2, price=9.99}, {sku=B2, qty=1, price=14.5}]", result.toString());
    }

    @Test
    @DisplayName("Should parse TOON format list array to JSON")
    void parseListArray() {
        setUpContext("[1]:\n  - first\n  - second\n  -");
        List<Object> result = ArrayDecoder.parseArray("[1]:\n  - first\n  - second\n  -", 0, context);
        assertEquals("""
            [- first
              - second
              -]""", result.toString());
    }

    @Test
    @DisplayName("Should extract the correct comma from delimiter")
    void expectsToExtractCommaFromDelimiter() {
        // Given
        setUpContext("items[3]: a,b,c");

        // When
        Delimiter result = ArrayDecoder.extractDelimiterFromHeader("items[3]: a,b,c", context);

        // Then
        assertEquals(",", result.toString());
    }
    @Test
    @DisplayName("Should extract the correct slash from delimiter")
    void expectsToExtractSlashFromDelimiter() {
        // Given
        setUpContext("items[3|]: a|b|c");

        // When
        Delimiter result = ArrayDecoder.extractDelimiterFromHeader("[3|]", context);

        // Then
        assertEquals("|", result.toString());
    }

    @Test
    @DisplayName("Should validate array length")
    void validateArrayLength() {
        assertThrows(IllegalArgumentException.class, () -> ArrayDecoder.validateArrayLength("[2]: 1,2,3", 3));
    }

    @Test
    @DisplayName("Should validate array length")
    void validateArrayLengthWithoutException() {
        assertDoesNotThrow(() -> ArrayDecoder.validateArrayLength("[2]: 1,2,3", 2));
    }

    @Test
    @DisplayName("Should split a array")
    void parseDelimitedValues() {
        // When
        List<String> strings = ArrayDecoder.parseDelimitedValues("1,2,3", Delimiter.COMMA);
        // Then
        assertEquals(3, strings.size());
    }

    @Test
    void shouldAddEmptyFinalValueWhenInputEndsWithDelimiter() {
        // When
        List<String> result = ArrayDecoder.parseDelimitedValues("a,b,", Delimiter.COMMA);

        // Then
        assertEquals(List.of("a", "b", ""), result);
    }

    @Test
    void shouldReturnEmptyListWhenInputIsEmpty() {
        // When
        List<String> result = ArrayDecoder.parseDelimitedValues("", Delimiter.COMMA);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("extract length from the Header")
    void extractLengthFromHeader() throws Exception {
        // Given
        String input = "[2]{sku,qty,price}:\n  A1,2,9.99\n  B2,1,14.5";

        // When
        Integer extractLengthFromHeader = (Integer) invokePrivateStatic("extractLengthFromHeader", new Class[]{String.class}, input);

        // Then
        assertEquals(2, extractLengthFromHeader);
    }

    @Test
    @DisplayName("extract length from the Header, for a Header without a number")
    void extractLengthFromHeaderNullReturn() throws Exception {
        // Given
        String input = "[T]{sku,qty,price}:\n  A1,2,9.99\n  B2,1,14.5";

        // When
        Integer extractLengthFromHeader = (Integer) invokePrivateStatic("extractLengthFromHeader", new Class[]{String.class}, input);

        // Then
        assertNull(extractLengthFromHeader);
    }

    @Test
    @DisplayName("do not terminate the List Array")
    void shouldTerminateListArrayReturnFalse() throws Exception {
        // Given
        setUpContext("items[3]: a,b,c");

        // When
        boolean terminateListArray = (boolean) invokePrivateStatic("shouldTerminateListArray", new Class[]{int.class, int.class, String.class, DecodeContext.class}, 3, 1, "    - item", this.context);

        // Then
        assertFalse(terminateListArray);
    }

    private void setUpContext(String toon) {
        this.context.lines = toon.split("\n", -1);
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter();
    }
}
