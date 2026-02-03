package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.PathExpansion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class TabularArrayDecoderTest {

    private final DecodeContext context = new DecodeContext();

    DecodeOptions before;

    @BeforeEach
    void setUp() {
        before = context.options;
    }

    @AfterEach
    void tearDown() {
        context.options = before;
    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<TabularArrayDecoder> constructor = TabularArrayDecoder.class.getDeclaredConstructor();
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
    @DisplayName("Parse TOON format tabular array to JSON")
    void parseTabularArray() {
        // Given
        setUpContext("[2]{id,value}:\n  1,null\n  2,\"test\"");

        // When
        List<Object> result = TabularArrayDecoder.parseTabularArray(
            "[2]{id,value}:\n  1,null\n  2,\"test\"", 0,
            Delimiter.COMMA, context);

        // Then
        assertEquals("[{id=1, value=null}, {id=2, value=test}]", result.toString());
    }

    @Test
    @DisplayName("Throws an exception if the wrong delimiter is being used")
    void inCaseOfMismatchInDelimiter_ThrowAnException() {
        // Given
        setUpContext("[2]{id,value}:\n  1,null\n  2,\"test\"");
        // When / then
        assertThrows(IllegalArgumentException.class, () -> TabularArrayDecoder.parseTabularArray(
            "[2]{id,value}:\n  1,null\n  2,\"test\"", 0,
            Delimiter.TAB, context));
    }

    @Test
    @DisplayName("processTabularRow: deeper-than-expected line is skipped (else-if branch)")
    void processTabularRow_skipsDeeperIndentedLine() {
        // Given
        String toon = "[2]{id,name}:\n  1,Ada\n    nested: true\n  2,Bob";

        setUpContext(toon);

        // When
        List<Object> result = TabularArrayDecoder.parseTabularArray(toon, 0,
            Delimiter.COMMA, context);

        // Then
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
    void testReturnsTrueWhenLineDepthLessThanExpected() throws Exception {
        // Given
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);


        String line = "  some text";   // Content irrelevant for this branch
        int lineDepth = 1;             // LESS than expectedRowDepth
        int expectedRowDepth = 3;       // Ensures we fall to final return

        List<String> keys = List.of("a", "b", "c");
        List<Object> result = new ArrayList<>();

        // When
        boolean processed = (boolean) invokePrivateStatic("processTabularRow",
            new Class[]{String.class, int.class, int.class, List.class, Delimiter.class, List.class, DecodeContext.class},
            line, lineDepth, expectedRowDepth,
            keys, Delimiter.COMMA, result, context
        );

        // Then
        assertTrue(processed, "Should return true when lineDepth < expectedRowDepth");
        assertTrue(result.isEmpty(), "Result list must remain unchanged");
    }

    @Test
    void testReturnsTrueWhenNextDepthIsHeaderOrLess() throws Exception {
        // Given

        // Lines in context:
        // line 0: blank
        // line 1: next non-blank line, with depth <= headerDepth
        String[] lines = {
            "",            // current line (blank)
            "key: value"   // next non-blank (depth 0)
        };

        context.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF);
        context.lines = lines;
        context.currentLine = 0;

        int expectedRowDepth = 2;


        // When
        boolean result = (boolean) invokePrivateStatic("handleBlankLineInTabularArray",
            new Class[]{int.class, DecodeContext.class},
            expectedRowDepth, context);

        // Than
        assertTrue(result, "Expected handleBlankLineInTabularArray to return true when nextDepth <= headerDepth");
    }

    @Test
    @DisplayName("validateKeysDelimiter get called and branches will be checked")
    void validateKeysDelimiter() throws Exception {
        // Given
        String keysStr = "sad\\a\"sd";

        // When / Then
        invokePrivateStatic("validateKeysDelimiter", new Class[]{String.class, Delimiter.class}, keysStr, Delimiter.COMMA);
    }

    @Test
    @DisplayName("validateKeysDelimiter get called and branches will be checked")
    void checkDelimiterMismatchExecution() {
        // Given
        String expectedChar = Delimiter.PIPE.toString();
        String actualChar = Delimiter.COMMA.toString();

        // When
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
            () -> invokePrivateStatic("checkDelimiterMismatch", new Class[]{char.class, char.class}, expectedChar.charAt(0), actualChar.charAt(0)));

        // Then
        assertNotNull(exception);
    }

    @Test
    @DisplayName("validateKeysDelimiter get called and branches will be checked")
    void checkDelimiterMismatchExecutionWithComa() {
        // Given
        String expectedChar = Delimiter.COMMA.toString();
        String actualChar = Delimiter.PIPE.toString();

        // When
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
            () -> invokePrivateStatic("checkDelimiterMismatch", new Class[]{char.class, char.class}, expectedChar.charAt(0), actualChar.charAt(0)));

        // Then
        assertNotNull(exception);
    }

    @Test
    void testTerminateWhenLineDepthLessThanExpected() throws Exception {
        // Given
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);

        String line = "    some value"; // Any line works; we won't reach colon logic.
        int lineDepth = 1;              // < expectedRowDepth
        int expectedRowDepth = 3;       // Must be > lineDepth

        // When
        boolean result = (boolean) invokePrivateStatic("shouldTerminateTabularArray",
            new Class[]{String.class, int.class, int.class, DecodeContext.class},
            line, lineDepth, expectedRowDepth, context);

        // Then
        assertTrue(result, "Should terminate when lineDepth < expectedRowDepth");
    }

    @Test
    void testParseTabularArray_ReturnsEmptyList_WhenHeaderDoesNotMatchPattern() {
        // Given
        context.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF);
        context.lines = new String[]{"ignored"};
        context.currentLine = 0;

        // When
        List<Object> result = TabularArrayDecoder.parseTabularArray(
            "not a header", // DOES NOT MATCH pattern
            0,
            Delimiter.COMMA,
            context
        );

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Expected empty list for non-matching header");
    }

    private void setUpContext(String toon) {
        this.context.lines = toon.split("\n", -1);
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter();
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method declaredMethod = TabularArrayDecoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }
}
