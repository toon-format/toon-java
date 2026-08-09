package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.PathExpansion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class DecodeHelperTest {

    private static final int TEST_CONFLICT_VALUE = 5;

    private final DecodeContext context = new DecodeContext();

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<DecodeHelper> constructor = DecodeHelper.class.getDeclaredConstructor();
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
    @DisplayName("Returns true or false if there's a blank line")
    void isBlankLine() {
        assertTrue(DecodeHelper.isBlankLine(""));
        assertFalse(DecodeHelper.isBlankLine("items[1]: \"\""));
    }

    @Test
    @DisplayName("Should find the correct depth for primitive arrays")
    void findDepthForPrimitiveArrays() {
        // Given
        setUpContext("tags[3]: 1,2,3");

        // When
        final int result = DecodeHelper.getDepth("tags[3]: 1,2,3", context);

        // Then
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should find the correct depth for nested arrays")
    void findDepthForNestedArrays() {
        // Given
        setUpContext("items[1]:\n  - id: 1\n    nested:\n      x: 1");

        // When
        final int result = DecodeHelper.getDepth("items[1]:\n  - id: 1\n    nested:\n      x: 1", context);

        // Then
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should find the correct depth for tabular arrays")
    void findDepthForTabularArrays() {
        // Given
        setUpContext("items[2]{\"order:id\",\"full name\"}:\n  1,Ada\n  2,Bob");

        // When
        final int result = DecodeHelper.getDepth("items[2]{\"order:id\",\"full name\"}:\n  1,Ada\n  2,Bob", context);

        // Then
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should find the correct depth for objects")
    void findDepthForObjects() {
        // Given
        setUpContext("id: 123\nname: Ada\nactive: true");

        // When
        final int result = DecodeHelper.getDepth("id: 123\nname: Ada\nactive: true", context);

        // Then
        assertEquals(0, result);
    }

    @Test
    @DisplayName("getDepth: throws on tab indentation in strict mode")
    void getDepth_throwsOnTabInStrict() {
        // Given
        setUpContext("\tkey: 1");
        context.currentLine = 0;

        // When
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.getDepth("\tkey: 1", context));

        // Then
        assertTrue(ex.getMessage().contains("Tab character"));
    }

    @Test
    @DisplayName("getDepth: throws on non-multiple spaces in strict mode")
    void getDepth_throwsOnNonMultipleIndent() {
        // Given
        setUpContext("   key: 1"); // 3 leading spaces, default indent=2
        context.currentLine = 0;

        // When
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.getDepth("   key: 1", context));

        // Then
        assertTrue(ex.getMessage().contains("Non-multiple indentation"));
    }

    @Test
    @DisplayName("getDepth: computes depth with custom indent size")
    void getDepth_withCustomIndent() {
        // Given
        // indent size 4, 8 spaces -> depth 2
        final int customIndent = 4;
        setUpContext("        key: 1");
        context.options = DecodeOptions.withIndent(customIndent);

        // When
        final int depth = DecodeHelper.getDepth("        key: 1", context);

        // Then
        assertEquals(2, depth);
    }

    @Test
    @DisplayName("getDepth: lenient mode allows tabs and odd spaces")
    void getDepth_lenientMode() {
        // Given
        setUpContext("\tkey: 1");
        context.options = DecodeOptions.withStrict(false);

        // When
        int depth = DecodeHelper.getDepth("\tkey: 1", context);

        // Then
        assertEquals(0, depth);
        // Odd number of spaces doesn't throw either
        depth = DecodeHelper.getDepth("   key: 1", context);
        assertEquals(1, depth); // leadingSpaces=3, indent default=2 -> integer division 1
    }

    @Test
    @DisplayName("findUnquotedColon: ignores colons inside quotes and handles escapes")
    void findUnquotedColon_case1() {
        // Given
        final String s1 = "\"order:id\": 1"; // first colon quoted, second is key-value

        // When
        final int idx1 = DecodeHelper.findUnquotedColon(s1);

        // Then
        assertEquals(s1.lastIndexOf(':'), idx1);
    }

    @Test
    void findUnquotedColon_case2() {
        // Given
        final String s2 = "no colon here";
        // When/Then
        assertEquals(-1, DecodeHelper.findUnquotedColon(s2));
    }

    @Test
    void findUnquotedColon_case() {
        // Given
        final String s3 = "\"escaped\\\"quote\":42"; // quoted section contains escaped quote

        // When
        final int idx3 = DecodeHelper.findUnquotedColon(s3);

        // Then
        assertEquals(s3.lastIndexOf(':'), idx3);
    }

    @Test
    @DisplayName("findNextNonBlankLine: skips blank lines")
    void findNextNonBlankLine_skipsBlanks() {
        // Given
        setUpContext("\n\n  a: 1\n\n");

        // When
        final int idx = DecodeHelper.findNextNonBlankLine(0, context);

        // Then
        assertEquals(2, idx);
    }

    @Test
    @DisplayName("findNextNonBlankLineDepth: returns depth or null when none")
    void findNextNonBlankLineDepth_cases() {
        // Given
        setUpContext("\n  a: 1\n\n");
        context.currentLine = 0;

        // When
        final Integer depth = DecodeHelper.findNextNonBlankLineDepth(context);

        // Then
        assertNotNull(depth);
        assertEquals(1, depth);
    }

    @Test
    @DisplayName("findNextNonBlankLineDepth: returns depth or null when none")
    void findNextNonBlankLineDepth_casesOnStrangeContent() {
        // Given
        setUpContext("\n\n");
        context.currentLine = 0;

        // When/Then
        assertNull(DecodeHelper.findNextNonBlankLineDepth(context));
    }

    @Test
    @DisplayName("validateNoMultiplePrimitivesAtRoot: throws when another root primitive follows")
    void validateNoMultiplePrimitivesAtRoot_throws() {
        // Given
        setUpContext("1\n2\n");
        context.currentLine = 0;

// When/Then
        assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.validateNoMultiplePrimitivesAtRoot(context));
    }

    @Test
    @DisplayName("checkFinalValueConflict: throws when existing is object/array and new is scalar (strict)")
    void checkFinalValueConflict_strictConflicts() {
        // Given
        setUpContext("");

        // When
        // object vs scalar
        final IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.checkFinalValueConflict("a", new java.util.HashMap<>(), 1, context));
        // Then
        assertTrue(ex1.getMessage().contains("object"));
    }

    @Test
    @DisplayName("checkFinalValueConflict: throws when existing is object/array and new is scalar (strict)")
    void checkFinalValueConflict_strictConflicts2() {
        // Given
        setUpContext("");

        // When
        // array vs scalar
        final IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.checkFinalValueConflict("a", new ArrayList<>(), 1, context));

        // Then
        assertTrue(ex2.getMessage().contains("array"));
    }

    @Test
    @DisplayName("checkPathExpansionConflict: respects strict mode toggle")
    void checkPathExpansionConflict_strictToggle() {
        // Given
        final Map<String, Object> map = new HashMap<>();
        map.put("a", new HashMap<>());

        // When
        // strict true -> conflict
        setUpContext("");
        assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.checkPathExpansionConflict(map, "a", 1, context));
    }

    @Test
    @DisplayName("checkPathExpansionConflict: respects strict mode toggle")
    void checkPathExpansionConflict_strictToggle2() {
        // Given
        final Map<String, Object> map = new HashMap<>();
        // strict false -> no conflict
        setUpContext("");
        context.options = DecodeOptions.withStrict(false);
        // When
        assertDoesNotThrow(() -> DecodeHelper.checkPathExpansionConflict(map, "a", 1, context));
    }

    @Nested
    @DisplayName("getDepth()")
    class GetDepthTests {

        int before;

        @BeforeEach
        void setUp() {
            before = context.currentLine;
        }

        @AfterEach
        void tearDown() {
            context.currentLine = before;
        }

        @Test
        @DisplayName("Given blank line, When getting depth, Then returns 0")
        void blankLineDepth() {
            // Given
            setUpContext(new String[]{"   "}, false, 2);

            // When / Then
            assertEquals(0, DecodeHelper.getDepth("   ", context));
        }

        @Test
        @DisplayName("Given strict mode and leading tab, Then throws exception")
        void strictTabThrows() {
            // Given
            setUpContext(new String[]{"\tabc"}, true, 2);
            context.currentLine = 0;

            // When / Then
            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.getDepth("\tabc", context));
        }

        @Test
        @DisplayName("Given strict mode and non-multiple indentation, Then throws")
        void strictNonMultipleIndentThrows() {
            // Given
            setUpContext(new String[]{"   abc"}, true, 2);
            context.currentLine = 0;

            // When / Then
            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.getDepth("   abc", context)); // 3 spaces not multiple of 2
        }

        @Test
        @DisplayName("Given non-strict mode and non-multiple indentation, Then allowed")
        void nonStrictAllowsNonMultiple() {
            setUpContext(new String[]{"   abc"}, false, 2);

            // When / Then
            assertEquals(1, DecodeHelper.getDepth("   abc", context)); // 3 / 2 -> 1
        }

        @Test
        @DisplayName("Given indentSize=0, Then return leading spaces")
        void indentZeroReturnsSpaces() {
            // Given
            setUpContext(new String[]{"    abc"}, false, 0);

            // When / Then
            final int expectedDepth = 4;
            assertEquals(expectedDepth, DecodeHelper.getDepth("    abc", context));
        }

        @Test
        @DisplayName("Given correct multiple indentation, Then correct depth returned")
        void correctDepth() {
            // Given
            setUpContext(new String[]{"    abc"}, true, 2);

            // When / Then
            assertEquals(2, DecodeHelper.getDepth("    abc", context));
        }

        @Test
        @DisplayName("Given strict mode and blank line with non-multiple spaces,"
                + " Then getDepth() throws (validateIndentation skipped)")
        void strictBlankNonMultipleIndentationThrows() {
            // Given
            // 3 spaces + NON-BREAKING SPACE (U+00A0)
            // NBSP is whitespace but NOT trimmed away and not counted as space -> perfect case
            final String line = "   " + (char) 0xA0;

            setUpContext(new String[]{line}, true, 2);
            context.currentLine = 0;
            // When
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.getDepth(line, context));

            // Then
            assertTrue(ex.getMessage().startsWith("Non-multiple indentation"));
        }

        @Test
        @DisplayName("validateIndentation: strict mode tab → throws")
        void validateIndentationTabThrows() {
            // Given
            final String line = "\tabc";

            setUpContext(new String[]{line}, true, 2);
            context.currentLine = 0;

            // When / Then
            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.getDepth(line, context));
        }

        @Test
        @DisplayName("validateIndentation: leading spaces then text → ok")
        void validateIndentationLeadingSpacesThenStop() {
            // Given
            final String line = "   abc"; // 3 spaces then letter

            final int validIndent = 3;
            setUpContext(new String[]{line}, true, validIndent); // indent 3 → valid
            context.currentLine = 0;

            // When / Then
            assertEquals(1, DecodeHelper.getDepth(line, context));
        }

        @Test
        @DisplayName("validateIndentation: strict + non-multiple indentation → throws")
        void validateIndentationNonMultipleThrows() {
            // Given
            final String line = "   abc"; // 3 spaces → not multiple of indent 2

            setUpContext(new String[]{line}, true, 2);
            context.currentLine = 0;
            // When
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.getDepth(line, context));


            // Then
            assertTrue(ex.getMessage().contains("Non-multiple indentation"));
        }

        @Test
        @DisplayName("validateIndentation: non-breaking space stops loop")
        void validateIndentationStopsAtNonBreakingSpace() {
            // Given
            final String line = "   " + (char) 0xA0 + "abc";
            final int customIndent = 3;

            setUpContext(new String[]{line}, true, customIndent);
            context.currentLine = 0;

            // When / Then
            // 3 spaces → valid multiple of 3
            assertEquals(1, DecodeHelper.getDepth(line, context));
        }
    }

    @Nested
    @DisplayName("findNextNonBlankLine()")
    class NextNonBlankTests {

        @Test
        void findNextNonBlank() {
            // Given
            setUpContext(new String[]{"", " ", "abc"}, false, 2);

            // When / Then
            assertEquals(2, DecodeHelper.findNextNonBlankLine(0, context));
        }

        @Test
        void noneFound() {
            // Given
            setUpContext(new String[]{"", "   "}, false, 2);

            // When / Then
            assertEquals(2, DecodeHelper.findNextNonBlankLine(0, context));
        }
    }

    @Nested
    @DisplayName("checkFinalValueConflict() and checkPathExpansionConflict()")
    class ConflictTests {

        @Test
        @DisplayName("Given strict mode and existing map but new primitive -> conflict")
        void mapToPrimitiveConflict() {
            // Given
            setUpContext(new String[]{}, true, 2);

            // When / Then
            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.checkFinalValueConflict("a", new HashMap<>(), TEST_CONFLICT_VALUE, context));
        }

        @Test
        @DisplayName("Given strict mode and existing list but new primitive -> conflict")
        void listToPrimitiveConflict() {
            // Given
            setUpContext(new String[]{}, true, 2);

            // When / Then
            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.checkFinalValueConflict("a", new ArrayList<>(), TEST_CONFLICT_VALUE, context));
        }

        @Test
        @DisplayName("Given non-strict mode -> no conflict")
        void nonStrictNoConflict() {
            // Given
            setUpContext(new String[]{}, false, 2);

            // When / Then
            assertDoesNotThrow(
                () -> DecodeHelper.checkFinalValueConflict("a", new HashMap<>(), TEST_CONFLICT_VALUE, context)
            );
        }

        @Test
        @DisplayName("checkPathExpansionConflict delegates to final conflict check")
        void pathExpansionConflict() {
            // Given
            setUpContext(new String[]{}, true, 2);
            final Map<String, Object> map = new HashMap<>();
            map.put("x", new HashMap<>());

            // When / Then
            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.checkPathExpansionConflict(map, "x", TEST_CONFLICT_VALUE, context));
        }
    }

    @Nested
    @DisplayName("validateNoMultiplePrimitivesAtRoot()")
    class ValidateRootTests {

        int before;

        @BeforeEach
        void setUp() {
            before = context.currentLine;
        }

        @AfterEach
        void tearDown() {
            context.currentLine = before;
        }

        @Test
        @DisplayName("Given next line at depth 0 in strict mode -> throw")
        void rootPrimitiveConflict() {
            // Given
            setUpContext(new String[]{"abc"}, true, 2);
            context.currentLine = 0;

            // When / Then
            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.validateNoMultiplePrimitivesAtRoot(context));
        }

        @Test
        @DisplayName("Given deeper indentation -> OK")
        void deeperIndentOk() {
            // Given
            setUpContext(new String[]{"  abc"}, true, 2);
            context.currentLine = 0;

            // When / Then
            assertDoesNotThrow(() -> DecodeHelper.validateNoMultiplePrimitivesAtRoot(context));
        }

        @Test
        @DisplayName("Given only blanks -> OK")
        void blanksOnlyOk() {
            // Given
            setUpContext(new String[]{"   "}, true, 2);
            context.currentLine = 0;

            // When / Then
            assertDoesNotThrow(() -> DecodeHelper.validateNoMultiplePrimitivesAtRoot(context));
        }
    }

    @Nested
    @DisplayName("hasUnquotedBrackets()")
    class HasUnquotedBrackets {

        @Test
        @DisplayName("should return true when brackets are present")
        void detectsBrackets() {
            assertTrue(DecodeHelper.hasUnquotedBrackets("foo[bar]"));
            assertTrue(DecodeHelper.hasUnquotedBrackets("[test]"));
            assertTrue(DecodeHelper.hasUnquotedBrackets("items[2]extra"));
        }

        @Test
        @DisplayName("should return false when no brackets")
        void noBrackets() {
            assertFalse(DecodeHelper.hasUnquotedBrackets("simple key: value"));
            assertFalse(DecodeHelper.hasUnquotedBrackets("foo"));
            assertFalse(DecodeHelper.hasUnquotedBrackets(""));
        }

        @Test
        @DisplayName("should return false when brackets are inside quotes")
        void bracketsInsideQuotes() {
            assertFalse(DecodeHelper.hasUnquotedBrackets("\"[test]\""));
            assertFalse(DecodeHelper.hasUnquotedBrackets("\"foo[bar]\""));
        }

        @Test
        @DisplayName("should handle escaped quotes properly")
        void escapedQuotes() {
            // escaped quote inside quoted section should not end the quotes
            assertFalse(DecodeHelper.hasUnquotedBrackets("\"escaped\\\"quote[br]\""));
        }
    }

    @Nested
    @DisplayName("checkDuplicateKey()")
    class CheckDuplicateKey {

        @Test
        @DisplayName("should throw when key already exists in strict mode")
        void duplicateKeyThrows() {
            final Map<String, Object> map = new HashMap<>();
            map.put("name", "Ada");
            setUpContext("");
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.checkDuplicateKey(map, "name", context));
            assertTrue(ex.getMessage().contains("Duplicate key"));
        }

        @Test
        @DisplayName("should not throw when key does not exist")
        void newKeyOk() {
            final Map<String, Object> map = new HashMap<>();
            map.put("name", "Ada");
            setUpContext("");
            assertDoesNotThrow(() -> DecodeHelper.checkDuplicateKey(map, "other", context));
        }

        @Test
        @DisplayName("should not throw in non-strict mode")
        void nonStrictAllowsDuplicate() {
            final Map<String, Object> map = new HashMap<>();
            map.put("name", "Ada");
            context.options = DecodeOptions.withStrict(false);
            assertDoesNotThrow(() -> DecodeHelper.checkDuplicateKey(map, "name", context));
        }
    }

    @Nested
    @DisplayName("computeLeadingSpaces()")
    class ComputeLeadingSpaces {
        DecodeContext ctxStrict2 = new DecodeContext();
        DecodeContext ctxNonStrict2 = new DecodeContext();
        DecodeContext ctxStrict4 = new DecodeContext();
        static final int strictIndent4 = 4;

        @BeforeEach
        void setup() {
            ctxStrict2.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
            ctxNonStrict2.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
            ctxStrict4.options = new DecodeOptions(strictIndent4, Delimiter.COMMA, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        }

        private int invokeCompute(final String line, final DecodeContext ctx) throws Exception {
            final Method declaredMethod = DecodeHelper.class.getDeclaredMethod("computeLeadingSpaces",
                    new Class<?>[]{String.class, DecodeContext.class});
            declaredMethod.setAccessible(true);

            return (int) declaredMethod.invoke(null, line, ctx);
        }

        @Test
        void testNoIndent() throws Exception {
            assertEquals(0, invokeCompute("abc", ctxStrict2));
            assertEquals(0, invokeCompute("", ctxStrict2));
        }

        @Test
        void testLeadingSpacesNonStrict() throws Exception {
            final int expectedSpaces3 = 3;
            final int expectedSpaces5 = 5;
            assertEquals(expectedSpaces3, invokeCompute("   hello", ctxNonStrict2));
            assertEquals(expectedSpaces5, invokeCompute("     x", ctxNonStrict2));
        }

        @Test
        void testLeadingSpacesStrictValidMultiple() throws Exception {
            final int fourSpacesResult = 4;
            assertEquals(2, invokeCompute("  x", ctxStrict2));
            assertEquals(fourSpacesResult, invokeCompute("    x", ctxStrict2));
        }

        @Test
        void testTabNonStrictStopsCounting() throws Exception {
            // Direct calls to computeLeadingSpaces stop at the first non-space.
            // The §12 tab leniency is applied earlier, in the ValueDecoder
            // pre-pass, where leading tabs are expanded to indentSize spaces.
            assertEquals(2, invokeCompute("  \t   text", ctxNonStrict2));
        }

        @Test
        void testStrictIndentSize4Valid() throws Exception {
            final int expectedSpaces4 = 4;
            final int expectedSpaces8 = 8;
            assertEquals(expectedSpaces4, invokeCompute("    x", ctxStrict4));
            assertEquals(expectedSpaces8, invokeCompute("        x", ctxStrict4));
        }

        @Test
        void testBlankLinesReturnZero() throws Exception {
            final int expectedSpaces4 = 4;
            final int expectedSpaces3 = 3;
            assertEquals(expectedSpaces4, invokeCompute("    ", ctxStrict2));
            assertEquals(0, invokeCompute("", ctxStrict2));
            assertEquals(expectedSpaces3, invokeCompute("   ", ctxNonStrict2));
        }
    }


    private void setUpContext(final String toon) {
        this.context.lines = toon.split("\n", -1);
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter();
    }

    private void setUpContext(final String[] lines, final boolean strict, final int indent) {
        this.context.lines = lines;
        this.context.options = new DecodeOptions(indent, Delimiter.COMMA, strict, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter();
    }
}
