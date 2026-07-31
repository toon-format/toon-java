package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.PathExpansion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class TabularArrayDecoderTest {

    private static final int SIMPLE_FIELD_COUNT = 3;

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
    @DisplayName("Parse tabular array and return unmodifiable list")
    void parseTabularArrayReturnsUnmodifiableList() {
        // Given
        setUpContext("[2]{id,value}:\n  1,null\n  2,\"test\"");

        // When
        final List<Object> result = TabularArrayDecoder.parseTabularArray(
            "[2]{id,value}:\n  1,null\n  2,\"test\"", 0,
            Delimiter.COMMA, context);

        // Then
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"));
    }

    @Test
    @DisplayName("Parse TOON format tabular array to JSON")
    void parseTabularArray() {
        // Given
        setUpContext("[2]{id,value}:\n  1,null\n  2,\"test\"");

        // When
        final List<Object> result = TabularArrayDecoder.parseTabularArray(
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
    @DisplayName("processTabularRow: deeper-than-expected line is skipped in lenient mode (else-if branch)")
    void processTabularRow_skipsDeeperIndentedLine() {
        // Given
        final String toon = "[2]{id,name}:\n  1,Ada\n    nested: true\n  2,Bob";

        setUpContext(toon);
        context.options = DecodeOptions.withStrict(false);

        // When
        final List<Object> result = TabularArrayDecoder.parseTabularArray(toon, 0,
            Delimiter.COMMA, context);

        // Then
        assertEquals(2, result.size(), "Should parse exactly two rows, skipping the deeper-indented line");

        @SuppressWarnings("unchecked")
        final Map<String, Object> row1 = (Map<String, Object>) result.get(0);
        @SuppressWarnings("unchecked")
        final Map<String, Object> row2 = (Map<String, Object>) result.get(1);

        assertEquals("1", String.valueOf(row1.get("id")));
        assertEquals("Ada", String.valueOf(row1.get("name")));

        assertEquals("2", String.valueOf(row2.get("id")));
        assertEquals("Bob", String.valueOf(row2.get("name")));
    }

    @Test
    void testReturnsTrueWhenLineDepthLessThanExpected() throws Exception {
        // Given
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);


        final String line = "  some text";   // Content irrelevant for this branch
        final int lineDepth = 1;             // LESS than expectedRowDepth
        final int expectedRowDepth = 3;       // Ensures we fall to final return

        final List<String> keys = List.of("a", "b", "c");
        final List<Object> result = new ArrayList<>();

        // When
        final boolean processed = (boolean) invokePrivateStatic("processTabularRow",
            new Class[]{String.class, int.class, int.class, List.class,
                Delimiter.class, List.class, DecodeContext.class},
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
        final String[] lines = {
            "",            // current line (blank)
            "key: value"   // next non-blank (depth 0)
        };

        context.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.lines = lines;
        context.currentLine = 0;

        final int expectedRowDepth = 2;


        // When
        final boolean result = (boolean) invokePrivateStatic("handleBlankLineInTabularArray",
            new Class[]{int.class, DecodeContext.class},
            expectedRowDepth, context);

        // Than
        assertTrue(result, "Expected handleBlankLineInTabularArray to return true when nextDepth <= headerDepth");
    }

    @Test
    @DisplayName("validateKeysDelimiter get called and branches will be checked")
    void validateKeysDelimiter() throws Exception {
        // Given
        final String keysStr = "sad\\a\"sd";

        // When / Then
        invokePrivateStatic("validateKeysDelimiter",
                new Class[]{String.class, Delimiter.class}, keysStr, Delimiter.COMMA);
    }

    @Test
    @DisplayName("validateKeysDelimiter get called and branches will be checked")
    void checkDelimiterMismatchExecution() {
        // Given
        final String expectedChar = Delimiter.PIPE.toString();
        final String actualChar = Delimiter.COMMA.toString();

        // When
        final InvocationTargetException exception = assertThrows(InvocationTargetException.class,
            () -> invokePrivateStatic("checkDelimiterMismatch",
                    new Class[]{char.class, char.class}, expectedChar.charAt(0), actualChar.charAt(0)));

        // Then
        assertNotNull(exception);
    }

    @Test
    @DisplayName("validateKeysDelimiter get called and branches will be checked")
    void checkDelimiterMismatchExecutionWithComa() {
        // Given
        final String expectedChar = Delimiter.COMMA.toString();
        final String actualChar = Delimiter.PIPE.toString();

        // When
        final InvocationTargetException exception = assertThrows(InvocationTargetException.class,
            () -> invokePrivateStatic("checkDelimiterMismatch",
                    new Class[]{char.class, char.class}, expectedChar.charAt(0), actualChar.charAt(0)));

        // Then
        assertNotNull(exception);
    }

    @Test
    void testTerminateWhenLineDepthLessThanExpected() throws Exception {
        // Given
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

        final String line = "    some value"; // Any line works; we won't reach colon logic.
        final int lineDepth = 1;              // < expectedRowDepth
        final int expectedRowDepth = 3;       // Must be > lineDepth

        // When
        final boolean result = (boolean) invokePrivateStatic("shouldTerminateTabularArray",
            new Class[]{String.class, int.class, int.class, DecodeContext.class},
            line, lineDepth, expectedRowDepth, context);

        // Then
        assertTrue(result, "Should terminate when lineDepth < expectedRowDepth");
    }

    @Test
    @DisplayName("should NOT terminate when delimiter found before colon (§9.3)")
    void testDisambiguation_DelimiterBeforeColon_continuesRow() throws Exception {
        // Given — "10,active:done" has comma at index 2, colon at index 9
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.delimiter = context.options.delimiter();
        final String line = "  10,active:done";
        final int lineDepth = 1;
        final int expectedRowDepth = 1;

        // When
        final boolean result = (boolean) invokePrivateStatic("shouldTerminateTabularArray",
            new Class[]{String.class, int.class, int.class, DecodeContext.class},
            line, lineDepth, expectedRowDepth, context);

        // Then — delimiter comes before colon, so this is a tabular row
        assertFalse(result, "Should continue tabular array when delimiter found before colon (§9.3)");
    }

    @Test
    @DisplayName("should terminate when colon found before delimiter (§9.3)")
    void testDisambiguation_ColonBeforeDelimiter_terminates() throws Exception {
        // Given — "time: 10,active" has colon at index 4, comma nowhere relevant
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.delimiter = context.options.delimiter();
        final String line = "  time: 10,active";
        final int lineDepth = 1;
        final int expectedRowDepth = 1;

        // When
        final boolean result = (boolean) invokePrivateStatic("shouldTerminateTabularArray",
            new Class[]{String.class, int.class, int.class, DecodeContext.class},
            line, lineDepth, expectedRowDepth, context);

        // Then — colon comes before any unquoted delimiter, so this is a key-value pair
        assertTrue(result, "Should terminate tabular array when colon found before delimiter (§9.3)");
    }

    @Test
    @DisplayName("should terminate when line has colon but no delimiter (§9.3)")
    void testDisambiguation_ColonOnly_terminates() throws Exception {
        // Given — "done: true" has colon but no comma delimiter
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.delimiter = context.options.delimiter();
        final String line = "  done: true";
        final int lineDepth = 1;
        final int expectedRowDepth = 1;

        // When
        final boolean result = (boolean) invokePrivateStatic("shouldTerminateTabularArray",
            new Class[]{String.class, int.class, int.class, DecodeContext.class},
            line, lineDepth, expectedRowDepth, context);

        // Then — colon present, no delimiter → key-value line
        assertTrue(result, "Should terminate tabular array when colon present without delimiter (§9.3)");
    }

    @Test
    @DisplayName("should NOT terminate when line has delimiter but no colon (§9.3)")
    void testDisambiguation_DelimiterOnly_continuesRow() throws Exception {
        // Given — "10,active" has comma but no colon → tabular row
        context.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.delimiter = context.options.delimiter();
        final String line = "  10,active";
        final int lineDepth = 1;
        final int expectedRowDepth = 1;

        // When
        final boolean result = (boolean) invokePrivateStatic("shouldTerminateTabularArray",
            new Class[]{String.class, int.class, int.class, DecodeContext.class},
            line, lineDepth, expectedRowDepth, context);

        // Then — no colon → this is a tabular row
        assertFalse(result, "Should continue tabular array when no colon present (§9.3)");
    }

    @Test
    @DisplayName("should handle tab pipe delimiter in disambiguation (§9.3)")
    void testDisambiguation_PipeDelimiter_continuesRow() throws Exception {
        // Given — pipe-delimited row, pipe before colon
        context.options = new DecodeOptions(2, Delimiter.PIPE, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.delimiter = context.options.delimiter();
        final String line = "  10|active:done";
        final int lineDepth = 1;
        final int expectedRowDepth = 1;

        // When
        final boolean result = (boolean) invokePrivateStatic("shouldTerminateTabularArray",
            new Class[]{String.class, int.class, int.class, DecodeContext.class},
            line, lineDepth, expectedRowDepth, context);

        // Then — pipe (delimiter) before colon → tabular row
        assertFalse(result, "Should continue tabular array with pipe delimiter when delim found before colon (§9.3)");
    }

    @Test
    @DisplayName("Parse simple field list into leaf nodes")
    void parseTabularKeys_givenSimpleList_thenLeafNodes() {
        // Given
        final DecodeContext ctx = new DecodeContext();
        ctx.options = DecodeOptions.DEFAULT;

        // When
        final List<TabularArrayDecoder.FieldNode> fields =
            TabularArrayDecoder.parseTabularKeys("a,b,c", Delimiter.COMMA, ctx);

        // Then
        assertEquals(SIMPLE_FIELD_COUNT, fields.size());
        assertEquals("a", fields.get(0).name());
        assertEquals("b", fields.get(1).name());
        assertEquals("c", fields.get(2).name());
        assertTrue(fields.get(0).children().isEmpty());
    }

    @Test
    @DisplayName("Parse backslash-escaped backslash inside a field name")
    void parseTabularKeys_givenEscapedBackslash_thenSingleField() {
        // Given
        final DecodeContext ctx = new DecodeContext();
        ctx.options = DecodeOptions.DEFAULT;

        // When
        final List<TabularArrayDecoder.FieldNode> fields =
            TabularArrayDecoder.parseTabularKeys("a\\\\b,c", Delimiter.COMMA, ctx);

        // Then
        assertEquals(2, fields.size());
        assertEquals("a\\b", fields.get(0).name());
        assertEquals("c", fields.get(1).name());
    }

    @Test
    @DisplayName("Parse quoted field name preserving delimiter characters")
    void parseTabularKeys_givenQuotedName_thenDelimiterPreserved() {
        // Given
        final DecodeContext ctx = new DecodeContext();
        ctx.options = DecodeOptions.DEFAULT;

        // When
        final List<TabularArrayDecoder.FieldNode> fields =
            TabularArrayDecoder.parseTabularKeys("\"a,b\",c", Delimiter.COMMA, ctx);

        // Then
        assertEquals(2, fields.size());
        assertEquals("a,b", fields.get(0).name());
        assertEquals("c", fields.get(1).name());
    }

    @Test
    @DisplayName("Parse nested field group into parent field with children")
    void parseTabularKeys_givenNestedGroup_thenParentWithChildren() {
        // Given
        final DecodeContext ctx = new DecodeContext();
        ctx.options = DecodeOptions.DEFAULT;

        // When
        final List<TabularArrayDecoder.FieldNode> fields =
            TabularArrayDecoder.parseTabularKeys("a{b,c},d", Delimiter.COMMA, ctx);

        // Then
        assertEquals(2, fields.size());
        assertEquals("a", fields.get(0).name());
        assertEquals(2, fields.get(0).children().size());
        assertEquals("b", fields.get(0).children().get(0).name());
        assertEquals("c", fields.get(0).children().get(1).name());
        assertEquals("d", fields.get(1).name());
    }

    @Test
    @DisplayName("Throw on unbalanced braces in strict mode")
    void parseTabularKeys_givenUnbalancedStrict_thenThrows() {
        // Given
        final DecodeContext ctx = new DecodeContext();
        ctx.options = DecodeOptions.DEFAULT;

        // When / Then
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> TabularArrayDecoder.parseTabularKeys("a{b,c", Delimiter.COMMA, ctx));
        assertTrue(ex.getMessage().contains("Unbalanced braces"));
    }

    @Test
    @DisplayName("Skip unbalanced group in lenient mode and keep parsed fields")
    void parseTabularKeys_givenUnbalancedLenient_thenPartialFields() {
        // Given
        final DecodeContext ctx = new DecodeContext();
        ctx.options = DecodeOptions.withStrict(false);

        // When
        final List<TabularArrayDecoder.FieldNode> fields =
            TabularArrayDecoder.parseTabularKeys("a{b,c", Delimiter.COMMA, ctx);

        // Then
        assertEquals(1, fields.size());
        assertEquals("a", fields.get(0).name());
    }

    @Test
    @DisplayName("Skip whitespace after delimiter in field list")
    void parseTabularKeys_givenWhitespaceAfterDelimiter_thenTrimmedFields() {
        // Given
        final DecodeContext ctx = new DecodeContext();
        ctx.options = DecodeOptions.DEFAULT;

        // When
        final List<TabularArrayDecoder.FieldNode> fields =
            TabularArrayDecoder.parseTabularKeys("a ,  b", Delimiter.COMMA, ctx);

        // Then
        assertEquals(2, fields.size());
        assertEquals("a", fields.get(0).name());
        assertEquals("b", fields.get(1).name());
    }

    @Test
    @DisplayName("Parse field list with pipe delimiter")
    void parseTabularKeys_givenPipeDelimiter_thenFields() {
        // Given
        final DecodeContext ctx = new DecodeContext();
        ctx.options = DecodeOptions.DEFAULT;

        // When
        final List<TabularArrayDecoder.FieldNode> fields =
            TabularArrayDecoder.parseTabularKeys("x|y", Delimiter.PIPE, ctx);

        // Then
        assertEquals(2, fields.size());
        assertEquals("x", fields.get(0).name());
        assertEquals("y", fields.get(1).name());
    }

    @Test
    void testParseTabularArray_ReturnsEmptyList_WhenHeaderDoesNotMatchPattern() {        // Given
        context.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        context.lines = new String[]{"ignored"};
        context.currentLine = 0;

        // When
        final List<Object> result = TabularArrayDecoder.parseTabularArray(
            "not a header", // DOES NOT MATCH pattern
            0,
            Delimiter.COMMA,
            context
        );

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Expected empty list for non-matching header");
    }

    private void setUpContext(final String toon) {
        this.context.lines = toon.split("\n", -1);
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter();
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(final String methodName, final Class<?>[] paramTypes,
            final Object... args) throws Exception {
        final Method declaredMethod = TabularArrayDecoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }
}
