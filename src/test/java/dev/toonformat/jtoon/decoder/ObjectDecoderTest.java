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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ObjectDecoderTest {

    private final DecodeContext context = new DecodeContext();

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<ObjectDecoder> constructor = ObjectDecoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    @DisplayName("Should parse scalar value to JSON")
    void parseBareScalarValue() {
        setUpContext("v: \"true\"");
        Object result = ObjectDecoder.parseBareScalarValue("v: \"true\"", 0, context);
        assertEquals("v: \"true\"", result.toString());
    }

    @Test
    @DisplayName("Should parse item value to JSON")
    void parseObjectItemValue() {
        setUpContext("note: \"a,b\"");
        Object result = ObjectDecoder.parseObjectItemValue("note: \"a,b\"", 0, context);
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
            // given
            setUpContext("""
                             parent:
                               child1: A
                               child2: B
                             """);

            context.currentLine = 1; // simulate: parser is already on the nested part

            // when
            Map<String, Object> result = ObjectDecoder.parseNestedObject(0, context);

            // then
            assertEquals("A", result.get("child1"));
            assertEquals("B", result.get("child2"));

            assertEquals(3, context.currentLine); // consumed all children
        }

        @Test
        @DisplayName("GIVEN deeper indentation WHEN child is not direct child THEN skip line")
        void parseNestedObject_skips_invalid_depth() {
            // given
            setUpContext("""
                             parent:
                                 tooDeep: X
                               child: OK
                             """);

            context.currentLine = 1;

            // when
            Map<String, Object> result = ObjectDecoder.parseNestedObject(0, context);

            // then
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
            setUpContext("""
                             a: 10
                             b: 20
                               nested: IGNORE
                             """);

            Map<String, Object> root = new LinkedHashMap<>();

            ObjectDecoder.parseRootObjectFields(root, 0, context);

            assertEquals(10L, root.get("a"));
            assertEquals(3, context.currentLine);
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
            setUpContext("123");

            Object result = ObjectDecoder.parseBareScalarValue("123", 0, context);

            assertEquals(123L, result);
            assertEquals(1, context.currentLine);
        }

        @Test
        @DisplayName("GIVEN strict mode WHEN multiple root primitives THEN exception")
        void parseBareScalarValue_multiple_primitives_strict() {
            setUpContext("""
                             42
                             99
                             """);

            context.options = DecodeOptions.withStrict(true);

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
            setUpContext("""
                             key:
                               a: 1
                               b: 2
                             """);

            context.currentLine = 0;

            Object value = ObjectDecoder.parseFieldValue("", 0, context);

            assertInstanceOf(Map.class, value);
            Map<String, Object> map = (Map<String, Object>) value;

            assertEquals(1L, map.get("a"));
            assertEquals(2L, map.get("b"));
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
                setUpContext("""
                                 - key:
                                     a: 5
                                     b: 6
                                 """);

                context.currentLine = 0;

                Object v = ObjectDecoder.parseObjectItemValue("", 0, context);

                assertInstanceOf(Map.class, v);
                Map<String, Object> map = (Map<String, Object>) v;
                assertNull(map.get("a"));
            }

            @Test
            @DisplayName("GIVEN primitive => primitive returned")
            void parseObjectItemValue_primitive() {
                setUpContext("- value");
                context.currentLine = 0;

                Object v = ObjectDecoder.parseObjectItemValue("value", 0, context);

                assertEquals("value", v);
            }

            @Test
            @DisplayName("GIVEN empty and no nested => empty map")
            void parseObjectItemValue_empty() {
                setUpContext("""
                                 -
                                 
                                 """);

                context.currentLine = 0;

                Object v = ObjectDecoder.parseObjectItemValue("", 0, context);

                assertInstanceOf(Map.class, v);
            }
        }

        @Test
        @DisplayName("GIVEN primitive string => primitive returned")
        void parseFieldValue_primitive() {
            setUpContext("key: 15");
            context.currentLine = 0;

            Object v = ObjectDecoder.parseFieldValue("15", 0, context);

            assertEquals(15L, v);
            assertEquals(1L, context.currentLine);
        }

        @Test
        @DisplayName("GIVEN empty and no nested => empty map")
        void parseFieldValue_empty_no_nested() {
            setUpContext("""
                             key:
                             next
                             """);
            context.currentLine = 0;

            Object v = ObjectDecoder.parseFieldValue("", 0, context);

            assertInstanceOf(Map.class, v);
        }
    }

    private void setUpContext(String toon) {
        this.context.lines = toon.split("\n");
        this.context.options = DecodeOptions.DEFAULT;
        this.context.delimiter = DecodeOptions.DEFAULT.delimiter().toString();
    }
}
