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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class TabularArrayDecoderTest {

    private final DecodeContext context = new DecodeContext();

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<TabularArrayDecoder> constructor = TabularArrayDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    @DisplayName("Parse TOON format tabular array to JSON")
    void parseTabularArray() {
        setUpContext("[2]{id,value}:\n  1,null\n  2,\"test\"");
        List<Object> result = TabularArrayDecoder.parseTabularArray(
            "[2]{id,value}:\n  1,null\n  2,\"test\"", 0,
            Delimiter.COMMA.toString(), context);
        assertEquals("[{id=1, value=null}, {id=2, value=test}]", result.toString());
    }

    @Test
    @DisplayName("Throws an exception if the wrong delimiter is being used")
    void inCaseOfMismatchInDelimiter_ThrowAnException() {
        setUpContext("[2]{id,value}:\n  1,null\n  2,\"test\"");
        assertThrows(IllegalArgumentException.class, () -> TabularArrayDecoder.parseTabularArray(
            "[2]{id,value}:\n  1,null\n  2,\"test\"", 0,
            Delimiter.TAB.toString(), context));
    }

    @Test
    @DisplayName("processTabularRow: deeper-than-expected line is skipped (else-if branch)")
    void processTabularRow_skipsDeeperIndentedLine() {
        // Arrange: header + one valid row, then a deeper-indented line, then another valid row
        // expected row depth (1) -> parsed as first row
        // deeper than expected (depth 2) -> should be skipped by else-if branch
        String toon = "[2]{id,name}:\n  1,Ada\n    nested: true\n  2,Bob";

        setUpContext(toon);

        // Act
        List<Object> result = TabularArrayDecoder.parseTabularArray(toon, 0,
                                                                    Delimiter.COMMA.toString(), context);

        // Assert: exactly two parsed rows and the deeper line ignored
        assertEquals(2, result.size(), "Should parse exactly two rows, skipping the deeper-indented line");

        @SuppressWarnings("unchecked")
        Map<String, Object> row1 = (Map<String, Object>) result.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> row2 = (Map<String, Object>) result.get(1);

        assertEquals("1", String.valueOf(row1.get("id")));
        assertEquals("Ada", String.valueOf(row1.get("name")));

        assertEquals("2", String.valueOf(row2.get("id")));
        assertEquals("Bob", String.valueOf(row2.get("name")));
    }

    @Test
    @DisplayName("validateKeysDelimiter get called and branches will be checked")
    void validateKeysDelimiter() throws Exception {
        // Given
        String keysStr = "sad\\a\"sd";
        String expectedDelimiter = ",";

        // When
        invokePrivateStatic("validateKeysDelimiter", new Class[] { String.class, String.class }, keysStr, expectedDelimiter);

        // Then
    }

    @Test
    @DisplayName("validateKeysDelimiter get called and branches will be checked")
    void checkDelimiterMismatchExecution() {
        // Given
        String expectedChar = "|";
        String actualChar = ",";

        // When
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                                                           () -> invokePrivateStatic("checkDelimiterMismatch", new Class[] { char.class, char.class }, expectedChar.charAt(0), actualChar.charAt(0)));

        // Then
        assertNotNull(exception);
    }

    @Test
    @DisplayName("validateKeysDelimiter get called and branches will be checked")
    void checkDelimiterMismatchExecutionWithComa() {
        // Given
        String expectedChar = ",";
        String actualChar = "|";

        // When
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                                                           () -> invokePrivateStatic("checkDelimiterMismatch", new Class[] { char.class, char.class }, expectedChar.charAt(0), actualChar.charAt(0)));

        // Then
        assertNotNull(exception);
    }

    private void setUpContext(String toon) {
        this.context.lines = toon.split("\n", -1);
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter().toString();
    }

    // Reflection helpers for invoking private static methods
    private static void invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method declaredMethod = TabularArrayDecoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        declaredMethod.invoke(null, args);
    }
}
