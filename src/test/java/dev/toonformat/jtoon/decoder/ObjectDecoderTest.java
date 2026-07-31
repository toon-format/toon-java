package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.util.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ObjectDecoderTest {

    private static final int CONSUMED_CHILD_LINES = 3;
    private static final long ROOT_A_VALUE = 10L;
    private static final long SCALAR_PARSE_VALUE = 15L;
    private static final long TEST_NUMBER_VALUE = 123L;

    private final DecodeContext context = new DecodeContext();

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<ObjectDecoder> constructor = ObjectDecoder.class.getDeclaredConstructor();
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
    @DisplayName("Should parse scalar value to JSON")
    void parseBareScalarValue() {
        // Given
        setUpContext("v: \"true\"");

        // When
        final Object result = ObjectDecoder.parseBareScalarValue("v: \"true\"", 0, context);

        // Then
        assertEquals("v: \"true\"", result.toString());
    }

    @Test
    @DisplayName("Should parse item value to JSON")
    void parseObjectItemValue() {
        // Given
        setUpContext("note: \"a,b\"");

        // When
        final Object result = ObjectDecoder.parseObjectItemValue("note: \"a,b\"", 0, context);

        // Then
        assertEquals("note: \"a,b\"", result.toString());
    }

    @Nested
    @DisplayName("parseNestedObject()")
    class ParseNestedObjectTests {

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
        @DisplayName("GIVEN nested structure WHEN parsing THEN nested map is returned")
        void parseNestedObject_basic() {
            // Given
            setUpContext("""
                parent:
                  child1: A
                  child2: B
                """);

            context.currentLine = 1; // simulate: parser is already on the nested part

            // When
            final Map<String, Object> result = ObjectDecoder.parseNestedObject(0, context);

            // Then
            assertEquals("A", result.get("child1"));
            assertEquals("B", result.get("child2"));

            assertEquals(CONSUMED_CHILD_LINES, context.currentLine); // consumed all children
        }

        @Test
        @DisplayName("GIVEN deeper indentation WHEN child is not direct child THEN skip line in lenient mode")
        void parseNestedObject_skips_invalid_depth() {
            // Given
            setUpContext("""
                parent:
                    tooDeep: X
                  child: OK
                """);
            context.options = DecodeOptions.withStrict(false);
            context.currentLine = 1;

            // When
            final Map<String, Object> result = ObjectDecoder.parseNestedObject(0, context);

            // Then
            assertEquals("OK", result.get("child"));
            assertEquals(CONSUMED_CHILD_LINES, context.currentLine);
        }
    }

    @Nested
    @DisplayName("parseRootObjectFields()")
    class ParseRootObjectFieldsTests {

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
        @DisplayName("GIVEN root kv WHEN parsing THEN map is filled")
        void parseRootObjectFields_basic() {
            // Given
            setUpContext("""
                a: 10
                b: 20
                  nested: IGNORE
                """);
            context.options = DecodeOptions.withStrict(false);
            final Map<String, Object> root = new LinkedHashMap<>();

            // When
            ObjectDecoder.parseRootObjectFields(root, 0, context);

            // Then
            assertEquals(ROOT_A_VALUE, root.get("a"));
            assertEquals(CONSUMED_CHILD_LINES, context.currentLine);
        }

        @Test
        void parseRootObjectFields_WithWrongDepth() {
            // Given
            setUpContext("""
                a: 10
                b: 20
                  nested: IGNORE
                """);
            final Map<String, Object> root = new LinkedHashMap<>();
            final int wrongDepth = 25;

            // When
            ObjectDecoder.parseRootObjectFields(root, wrongDepth, context);

            // Then
            assertNull(root.get("a"));
            assertEquals(0, context.currentLine);
        }
    }

    @Nested
    @DisplayName("parseBareScalarValue()")
    class ParseBareScalarValueTests {

        int before;
        DecodeOptions beforeOptions;

        @BeforeEach
        void setUp() {
            before = context.currentLine;
            beforeOptions = context.options;
        }

        @AfterEach
        void tearDown() {
            context.currentLine = before;
            context.options = beforeOptions;
        }

        @Test
        @DisplayName("GIVEN primitive WHEN parsing THEN returned and currentLine++")
        void parseBareScalarValue_basic() {
            // Given
            setUpContext("123");

            // When
            final Object result = ObjectDecoder.parseBareScalarValue("123", 0, context);

            // Then
            assertEquals(TEST_NUMBER_VALUE, result);
            assertEquals(1, context.currentLine);
        }

        @Test
        @DisplayName("GIVEN strict mode WHEN multiple root primitives THEN exception")
        void parseBareScalarValue_multiple_primitives_strict() {
            // Given
            setUpContext("""
                42
                99
                """);
            context.options = DecodeOptions.withStrict(true);

            // When / then
            assertThrows(IllegalArgumentException.class, () ->
                ObjectDecoder.parseBareScalarValue("99", 0, context));
        }
    }

    @Nested
    @DisplayName("parseFieldValue()")
    class ParseFieldValueTests {

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
        @DisplayName("GIVEN empty value + nested => nested map parsed")
        @SuppressWarnings("unchecked")
        void parseFieldValue_nested() {
            // Given
            setUpContext("""
                key:
                  a: 1
                  b: 2
                """);
            context.currentLine = 0;

            // When
            final Object value = ObjectDecoder.parseFieldValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, value);

            final Map<String, Object> map = (Map<String, Object>) value;
            assertEquals(1L, map.get("a"));
            assertEquals(2L, map.get("b"));
        }


        @Test
        @DisplayName("GIVEN primitive string => primitive returned")
        void parseFieldValue_primitive() {
            // Given
            setUpContext("key: 15");
            context.currentLine = 0;

            // When
            final Object parseFieldValue = ObjectDecoder.parseFieldValue("15", 0, context);

            // Then
            assertEquals(SCALAR_PARSE_VALUE, parseFieldValue);
            assertEquals(1L, context.currentLine);
        }

        @Test
        @DisplayName("GIVEN empty and no nested => empty map")
        void parseFieldValue_empty_no_nested() {
            // Given
            setUpContext("""
                key:
                next
                """);
            context.currentLine = 0;

            // When
            final Object parseFieldValue = ObjectDecoder.parseFieldValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, parseFieldValue);
            assertEquals(1, context.currentLine);
        }

        @Test
        @DisplayName("GIVEN empty and no nested => empty map")
        void parseFieldValue_empty_no_nestedButBigCurrentLine() {
            // Given
            setUpContext("""
                key:
                next
                """);
            final int offset = 25;
            context.currentLine = offset;

            // When
            final Object parseFieldValue = ObjectDecoder.parseFieldValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, parseFieldValue);
            assertEquals(offset + 1, context.currentLine);
        }

        @Test
        @DisplayName("GIVEN inline value + deeper line + strict => over-indented exception")
        void parseFieldValue_inlineValueWithDeeperLineThrowsInStrictMode() {
            // Given
            setUpContext("""
                key: 15
                  orphan
                """);
            context.currentLine = 0;

            // When / Then
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ObjectDecoder.parseFieldValue("15", 0, context));
            assertTrue(ex.getMessage().contains("Over-indented line"));
        }

        @Test
        @DisplayName("GIVEN inline value + deeper line + lenient => value kept, orphan lines skipped")
        void parseFieldValue_inlineValueWithDeeperLineSkippedInLenientMode() {
            // Given
            setUpContext("""
                key: 15
                  orphan
                """);
            context.options = DecodeOptions.withStrict(false);
            context.currentLine = 0;

            // When
            final Object parseFieldValue = ObjectDecoder.parseFieldValue("15", 0, context);

            // Then
            assertEquals(SCALAR_PARSE_VALUE, parseFieldValue);
            assertEquals(2, context.currentLine);
        }

        @Test
        @DisplayName("GIVEN bracket pair value => parsed as string, not empty list")
        void parseFieldValue_bracketPairStaysString() {
            // Given
            setUpContext("key: []");
            context.currentLine = 0;

            // When
            final Object parseFieldValue = ObjectDecoder.parseFieldValue("[]", 0, context);

            // Then
            assertEquals("[]", parseFieldValue);
            assertEquals(1, context.currentLine);
        }

        @Test
        @DisplayName("GIVEN blank value on last line => empty map")
        void parseFieldValue_blankValueOnLastLineBecomesEmptyMap() {
            // Given
            setUpContext("key: ");
            context.currentLine = 0;

            // When
            final Object parseFieldValue = ObjectDecoder.parseFieldValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, parseFieldValue);
            assertTrue(((Map<?, ?>) parseFieldValue).isEmpty());
            assertEquals(1, context.currentLine);
        }
    }

    @Nested
    @DisplayName("parseObjectItemValue()")
    class ParseObjectItemValueTests {

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
        @DisplayName("GIVEN empty + nested => nested map")
        @SuppressWarnings("unchecked")
        void parseObjectItemValue_nested() {
            // Given
            setUpContext("""
                - key:
                    a: 5
                    b: 6
                """);
            context.currentLine = 0;

            // When
            final Object parseObjectItemValue = ObjectDecoder.parseObjectItemValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, parseObjectItemValue);
            final Map<String, Object> map = (Map<String, Object>) parseObjectItemValue;
            assertNull(map.get("a"));
        }

        @Test
        @DisplayName("GIVEN primitive => primitive returned")
        void parseObjectItemValue_primitive() {
            // Given
            setUpContext("- value");
            context.currentLine = 0;

            // When
            final Object parseObjectItemValue = ObjectDecoder.parseObjectItemValue("value", 0, context);

            // Then
            assertEquals("value", parseObjectItemValue);
        }

        @Test
        @DisplayName("GIVEN empty and no nested => empty map")
        void parseObjectItemValue_empty() {
            // Given
            setUpContext("""
                -
                
                """);
            context.currentLine = 0;

            // When
            final Object parseObjectItemValue = ObjectDecoder.parseObjectItemValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, parseObjectItemValue);
        }

        @Test
        @DisplayName("GIVEN empty and no nested => empty map")
        void parseObjectItemValue_emptyContext() {
            // Given
            setUpContext("\n\n");
            context.currentLine = 0;

            // When
            final Object parseObjectItemValue = ObjectDecoder.parseObjectItemValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, parseObjectItemValue);
        }
    }

    @Test
    void testExpandPathIntoMapCalledForDottedKey() throws Exception {
        // Given
        final Map<String, Object> objectMap = new LinkedHashMap<>();
        final String content = "user.name[1]: Alice";

        final int depth = 0;

        setUpContext(content);

        // When
        invokePrivateStatic(
            "processRootKeyedArrayLine",
            new Class[]{Map.class, String.class, Headers.KeyedHeaderMatch.class, int.class, DecodeContext.class},
            objectMap, content, Headers.matchKeyedArrayHeader(content), depth, context);

        // Then
        assertTrue(objectMap.containsKey("user.name"));
        assertEquals(List.of("Alice"), objectMap.get("user.name"));
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(final String methodName, final Class<?>[] paramTypes,
            final Object... args) throws Exception {
        final Method declaredMethod = ObjectDecoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }

    private void setUpContext(final String toon) {
        this.context.lines = toon.split("\n");
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter();
    }
}
