package dev.toonformat.jtoon.decoder;

import dev.toonformat.jtoon.DecodeOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ObjectDecoderTest {

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
        Object result = ObjectDecoder.parseBareScalarValue("v: \"true\"", 0, context);

        // Then
        assertEquals("v: \"true\"", result.toString());
    }

    @Test
    @DisplayName("Should parse item value to JSON")
    void parseObjectItemValue() {
        // Given
        setUpContext("note: \"a,b\"");

        // When
        Object result = ObjectDecoder.parseObjectItemValue("note: \"a,b\"", 0, context);

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
            Map<String, Object> result = ObjectDecoder.parseNestedObject(0, context);

            // Then
            assertEquals("A", result.get("child1"));
            assertEquals("B", result.get("child2"));

            assertEquals(3, context.currentLine); // consumed all children
        }

        @Test
        @DisplayName("GIVEN deeper indentation WHEN child is not direct child THEN skip line")
        void parseNestedObject_skips_invalid_depth() {
            // Given
            setUpContext("""
                parent:
                    tooDeep: X
                  child: OK
                """);

            context.currentLine = 1;

            // When
            Map<String, Object> result = ObjectDecoder.parseNestedObject(0, context);

            // Then
            assertEquals("OK", result.get("child"));
            assertEquals(3, context.currentLine);
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
            Map<String, Object> root = new LinkedHashMap<>();

            // When
            ObjectDecoder.parseRootObjectFields(root, 0, context);

            // Then
            assertEquals(10L, root.get("a"));
            assertEquals(3, context.currentLine);
        }

        @Test
        void parseRootObjectFields_WithWrongDepth() {
            // Given
            setUpContext("""
                a: 10
                b: 20
                  nested: IGNORE
                """);
            Map<String, Object> root = new LinkedHashMap<>();

            // When
            ObjectDecoder.parseRootObjectFields(root, 25, context);

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
            Object result = ObjectDecoder.parseBareScalarValue("123", 0, context);

            // Then
            assertEquals(123L, result);
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
            Object value = ObjectDecoder.parseFieldValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, value);

            Map<String, Object> map = (Map<String, Object>) value;
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
            Object parseFieldValue = ObjectDecoder.parseFieldValue("15", 0, context);

            // Then
            assertEquals(15L, parseFieldValue);
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
            Object parseFieldValue = ObjectDecoder.parseFieldValue("", 0, context);

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
            context.currentLine = 25;

            // When
            Object parseFieldValue = ObjectDecoder.parseFieldValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, parseFieldValue);
            assertEquals(26, context.currentLine);
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
            Object parseObjectItemValue = ObjectDecoder.parseObjectItemValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, parseObjectItemValue);
            Map<String, Object> map = (Map<String, Object>) parseObjectItemValue;
            assertNull(map.get("a"));
        }

        @Test
        @DisplayName("GIVEN primitive => primitive returned")
        void parseObjectItemValue_primitive() {
            // Given
            setUpContext("- value");
            context.currentLine = 0;

            // When
            Object parseObjectItemValue = ObjectDecoder.parseObjectItemValue("value", 0, context);

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
            Object parseObjectItemValue = ObjectDecoder.parseObjectItemValue("", 0, context);

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
            Object parseObjectItemValue = ObjectDecoder.parseObjectItemValue("", 0, context);

            // Then
            assertInstanceOf(Map.class, parseObjectItemValue);
        }
    }

    @Test
    void testExpandPathIntoMapCalledForDottedKey() throws Exception {
        // Given
        Map<String, Object> objectMap = new LinkedHashMap<>();
        String content = "user.name[1]: Alice";

        int depth = 0;

        setUpContext(content);

        // When
        invokePrivateStatic(
            "processRootKeyedArrayLine", new Class[]{Map.class, String.class, String.class, int.class, DecodeContext.class},
            objectMap, content, "user.name", depth, context);

        // Then
        assertTrue(objectMap.containsKey("user.name"));
        assertEquals(List.of("Alice"), objectMap.get("user.name"));
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method declaredMethod = ObjectDecoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }

    private void setUpContext(String toon) {
        this.context.lines = toon.split("\n");
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter();
    }
}
