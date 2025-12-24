package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.PathExpansion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class DecodeHelperTest {
    private final DecodeContext context = new DecodeContext();

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<DecodeHelper> constructor = DecodeHelper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

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
        setUpContext("tags[3]: 1,2,3");
        int result = DecodeHelper.getDepth("tags[3]: 1,2,3", context);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should find the correct depth for nested arrays")
    void findDepthForNestedArrays() {
        setUpContext("items[1]:\n  - id: 1\n    nested:\n      x: 1");
        int result = DecodeHelper.getDepth("items[1]:\n  - id: 1\n    nested:\n      x: 1", context);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should find the correct depth for tabular arrays")
    void findDepthForTabularArrays() {
        setUpContext("items[2]{\"order:id\",\"full name\"}:\n  1,Ada\n  2,Bob");
        int result = DecodeHelper.getDepth("items[2]{\"order:id\",\"full name\"}:\n  1,Ada\n  2,Bob", context);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should find the correct depth for objects")
    void findDepthForObjects() {
        setUpContext("id: 123\nname: Ada\nactive: true");
        int result = DecodeHelper.getDepth("id: 123\nname: Ada\nactive: true", context);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("getDepth: throws on tab indentation in strict mode")
    void getDepth_throwsOnTabInStrict() {
        setUpContext("\tkey: 1");
        context.currentLine = 0;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.getDepth("\tkey: 1", context));
        assertTrue(ex.getMessage().contains("Tab character"));
    }

    @Test
    @DisplayName("getDepth: throws on non-multiple spaces in strict mode")
    void getDepth_throwsOnNonMultipleIndent() {
        setUpContext("   key: 1"); // 3 leading spaces, default indent=2
        context.currentLine = 0;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.getDepth("   key: 1", context));
        assertTrue(ex.getMessage().contains("Non-multiple indentation"));
    }

    @Test
    @DisplayName("getDepth: computes depth with custom indent size")
    void getDepth_withCustomIndent() {
        // indent size 4, 8 spaces -> depth 2
        setUpContext("        key: 1");
        context.options = DecodeOptions.withIndent(4);
        int depth = DecodeHelper.getDepth("        key: 1", context);
        assertEquals(2, depth);
    }

    @Test
    @DisplayName("getDepth: lenient mode allows tabs and odd spaces")
    void getDepth_lenientMode() {
        // Make it lenient
        setUpContext("\tkey: 1");
        context.options = DecodeOptions.withStrict(false);
        int depth = DecodeHelper.getDepth("\tkey: 1", context);
        assertEquals(0, depth);
        // Odd number of spaces doesn't throw either
        depth = DecodeHelper.getDepth("   key: 1", context);
        assertEquals(1, depth); // leadingSpaces=3, indent default=2 -> integer division 1
    }

    @Test
    @DisplayName("findUnquotedColon: ignores colons inside quotes and handles escapes")
    void findUnquotedColon_cases() {
        String s1 = "\"order:id\": 1"; // first colon quoted, second is key-value
        int idx1 = DecodeHelper.findUnquotedColon(s1);
        assertEquals(s1.lastIndexOf(':'), idx1);

        String s2 = "no colon here";
        assertEquals(-1, DecodeHelper.findUnquotedColon(s2));

        String s3 = "\"escaped\\\"quote\":42"; // quoted section contains escaped quote
        int idx3 = DecodeHelper.findUnquotedColon(s3);
        assertEquals(s3.lastIndexOf(':'), idx3);
    }

    @Test
    @DisplayName("findNextNonBlankLine: skips blank lines")
    void findNextNonBlankLine_skipsBlanks() {
        setUpContext("\n\n  a: 1\n\n");
        int idx = DecodeHelper.findNextNonBlankLine(0, context);
        assertEquals(2, idx);
    }

    @Test
    @DisplayName("findNextNonBlankLineDepth: returns depth or null when none")
    void findNextNonBlankLineDepth_cases() {
        setUpContext("\n  a: 1\n\n");
        context.currentLine = 0;
        Integer depth = DecodeHelper.findNextNonBlankLineDepth(context);
        assertNotNull(depth);
        assertEquals(1, depth);
    }

    @Test
    @DisplayName("findNextNonBlankLineDepth: returns depth or null when none")
    void findNextNonBlankLineDepth_casesOnStrangeContent() {
        setUpContext("\n\n");
        context.currentLine = 0;
        assertNull(DecodeHelper.findNextNonBlankLineDepth(context));
    }

    @Test
    @DisplayName("validateNoMultiplePrimitivesAtRoot: throws when another root primitive follows")
    void validateNoMultiplePrimitivesAtRoot_throws() {
        setUpContext("1\n2\n");
        context.currentLine = 0;
        assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.validateNoMultiplePrimitivesAtRoot(context));
    }

    @Test
    @DisplayName("checkFinalValueConflict: throws when existing is object/array and new is scalar (strict)")
    void checkFinalValueConflict_strictConflicts() {
        setUpContext("");
        // object vs scalar
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.checkFinalValueConflict("a", new java.util.HashMap<>(), 1, context));
        assertTrue(ex1.getMessage().contains("object"));
        // array vs scalar
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.checkFinalValueConflict("a", new java.util.ArrayList<>(), 1, context));
        assertTrue(ex2.getMessage().contains("array"));
    }

    @Test
    @DisplayName("checkPathExpansionConflict: respects strict mode toggle")
    void checkPathExpansionConflict_strictToggle() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("a", new java.util.HashMap<>());

        // strict true -> conflict
        setUpContext("");
        assertThrows(IllegalArgumentException.class,
            () -> DecodeHelper.checkPathExpansionConflict(map, "a", 1, context));

        // strict false -> no conflict
        setUpContext("");
        context.options = DecodeOptions.withStrict(false);
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
            setUpContext(new String[]{"   "}, false, 2);
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
            assertEquals(1, DecodeHelper.getDepth("   abc", context)); // 3 / 2 -> 1
        }

        @Test
        @DisplayName("Given indentSize=0, Then return leading spaces")
        void indentZeroReturnsSpaces() {
            setUpContext(new String[]{"    abc"}, false, 0);
            assertEquals(4, DecodeHelper.getDepth("    abc", context));
        }

        @Test
        @DisplayName("Given correct multiple indentation, Then correct depth returned")
        void correctDepth() {
            setUpContext(new String[]{"    abc"}, true, 2);
            assertEquals(2, DecodeHelper.getDepth("    abc", context));
        }

        @Test
        @DisplayName("Given strict mode and blank line with non-multiple spaces, Then getDepth() throws (validateIndentation skipped)")
        void strictBlankNonMultipleIndentationThrows() {
            // 3 spaces + NON-BREAKING SPACE (U+00A0)
            // NBSP is whitespace but NOT trimmed away and not counted as space -> perfect case
            String line = "   \u00A0";

            setUpContext(new String[]{line}, true, 2);
            context.currentLine = 0;

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.getDepth(line, context));

            assertTrue(ex.getMessage().startsWith("Non-multiple indentation"));
        }

        @Test
        @DisplayName("validateIndentation: strict mode tab → throws")
        void validateIndentationTabThrows() {
            String line = "\tabc";

            setUpContext(new String[]{line}, true, 2);
            context.currentLine = 0;

            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.getDepth(line, context));
        }

        @Test
        @DisplayName("validateIndentation: leading spaces then text → ok")
        void validateIndentationLeadingSpacesThenStop() {
            String line = "   abc"; // 3 spaces then letter

            setUpContext(new String[]{line}, true, 3); // indent 3 → valid
            context.currentLine = 0;

            assertEquals(1, DecodeHelper.getDepth(line, context));
        }

        @Test
        @DisplayName("validateIndentation: strict + non-multiple indentation → throws")
        void validateIndentationNonMultipleThrows() {
            String line = "   abc"; // 3 spaces → not multiple of indent 2

            setUpContext(new String[]{line}, true, 2);
            context.currentLine = 0;

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.getDepth(line, context));

            assertTrue(ex.getMessage().contains("Non-multiple indentation"));
        }

        @Test
        @DisplayName("validateIndentation: non-breaking space stops loop")
        void validateIndentationStopsAtNonBreakingSpace() {
            String line = "   \u00A0abc";

            setUpContext(new String[]{line}, true, 3);
            context.currentLine = 0;

            // 3 spaces → valid multiple of 3
            assertEquals(1, DecodeHelper.getDepth(line, context));
        }
    }

    @Nested
    @DisplayName("findNextNonBlankLine()")
    class NextNonBlankTests {

        @Test
        void findNextNonBlank() {
            setUpContext(new String[]{"", " ", "abc"}, false, 2);
            assertEquals(2, DecodeHelper.findNextNonBlankLine(0, context));
        }

        @Test
        void noneFound() {
            setUpContext(new String[]{"", "   "}, false, 2);
            assertEquals(2, DecodeHelper.findNextNonBlankLine(0, context));
        }
    }

    @Nested
    @DisplayName("checkFinalValueConflict() and checkPathExpansionConflict()")
    class ConflictTests {

        @Test
        @DisplayName("Given strict mode and existing map but new primitive -> conflict")
        void mapToPrimitiveConflict() {
            setUpContext(new String[]{}, true, 2);

            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.checkFinalValueConflict("a", new HashMap<>(), 5, context));
        }

        @Test
        @DisplayName("Given strict mode and existing list but new primitive -> conflict")
        void listToPrimitiveConflict() {
            setUpContext(new String[]{}, true, 2);
            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.checkFinalValueConflict("a", new ArrayList<>(), 5, context));
        }

        @Test
        @DisplayName("Given non-strict mode -> no conflict")
        void nonStrictNoConflict() {
            setUpContext(new String[]{}, false, 2);
            assertDoesNotThrow(
                () -> DecodeHelper.checkFinalValueConflict("a", new HashMap<>(), 5, context)
            );
        }

        @Test
        @DisplayName("checkPathExpansionConflict delegates to final conflict check")
        void pathExpansionConflict() {
            setUpContext(new String[]{}, true, 2);
            Map<String, Object> map = new HashMap<>();
            map.put("x", new HashMap<>());

            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.checkPathExpansionConflict(map, "x", 5, context));
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
            setUpContext(new String[]{"abc"}, true, 2);
            context.currentLine = 0;
            assertThrows(IllegalArgumentException.class,
                () -> DecodeHelper.validateNoMultiplePrimitivesAtRoot(context));
        }

        @Test
        @DisplayName("Given deeper indentation -> OK")
        void deeperIndentOk() {
            setUpContext(new String[]{"  abc"}, true, 2);
            context.currentLine = 0;
            assertDoesNotThrow(() -> DecodeHelper.validateNoMultiplePrimitivesAtRoot(context));
        }

        @Test
        @DisplayName("Given only blanks -> OK")
        void blanksOnlyOk() {
            setUpContext(new String[]{"   "}, true, 2);
            context.currentLine = 0;
            assertDoesNotThrow(() -> DecodeHelper.validateNoMultiplePrimitivesAtRoot(context));
        }
    }

    @Nested
    @DisplayName("computeLeadingSpaces()")
    class computeLeadingSpaces {
        DecodeContext ctxStrict2 = new DecodeContext();
        DecodeContext ctxNonStrict2 = new DecodeContext();
        DecodeContext ctxStrict4 = new DecodeContext();

        @BeforeEach
        void setup() {
            ctxStrict2.options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF);
            ctxNonStrict2.options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF);
            ctxStrict4.options = new DecodeOptions(4, Delimiter.COMMA, true, PathExpansion.OFF);
        }

        private int invokeCompute(String line, DecodeContext ctx) throws Exception {
            Method declaredMethod = DecodeHelper.class.getDeclaredMethod("computeLeadingSpaces", new Class<?>[]{String.class, DecodeContext.class});
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
            assertEquals(3, invokeCompute("   hello", ctxNonStrict2));
            assertEquals(5, invokeCompute("     x", ctxNonStrict2));
        }

        @Test
        void testLeadingSpacesStrictValidMultiple() throws Exception {
            assertEquals(2, invokeCompute("  x", ctxStrict2));
            assertEquals(4, invokeCompute("    x", ctxStrict2));
        }

        @Test
        void testTabNonStrictStopsCounting() throws Exception {
            // in non-strict mode, indentation stops at first non-space (including tab)
            assertEquals(2, invokeCompute("  \t   text", ctxNonStrict2));
        }

        @Test
        void testStrictIndentSize4Valid() throws Exception {
            assertEquals(4, invokeCompute("    x", ctxStrict4));
            assertEquals(8, invokeCompute("        x", ctxStrict4));
        }

        @Test
        void testBlankLinesReturnZero() throws Exception {
            assertEquals(4, invokeCompute("    ", ctxStrict2));
            assertEquals(0, invokeCompute("", ctxStrict2));
            assertEquals(3, invokeCompute("   ", ctxNonStrict2));
        }
    }


    private void setUpContext(String toon) {
        this.context.lines = toon.split("\n", -1);
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter().toString();
    }

    private void setUpContext(String[] lines, boolean strict, int indent) {
        this.context.lines = lines;
        this.context.options = new DecodeOptions(indent, Delimiter.COMMA, strict, PathExpansion.OFF);
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter().toString();
    }
}
