package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArrayEncoderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

    @Test
    void isArrayOfPrimitivesTestWithObjectNode() {
        // Given
        ObjectNode dataTable = MAPPER.createObjectNode();

        // When
        boolean arrayOfArrays = ArrayEncoder.isArrayOfPrimitives(dataTable);

        // Then
        assertFalse(arrayOfArrays);
    }

    @Test
    @DisplayName("given array-of-arrays with mixed inner types when encodeArrayOfArraysAsListItems then writes header and primitive list items")
    void givenArrayOfArraysWithMixedInnerTypes_whenEncodePrivate_thenWritesExpected() throws Exception {
        // Given
        ArrayNode outer = jsonNodeFactory.arrayNode();

        ArrayNode innerPrims = jsonNodeFactory.arrayNode().add(1).add(2);
        ArrayNode innerObjects = jsonNodeFactory.arrayNode();
        innerObjects.add(jsonNodeFactory.objectNode().put("a", 1));
        outer.add(innerPrims).add(innerObjects).add("x");

        EncodeOptions options = EncodeOptions.DEFAULT;
        LineWriter writer = new LineWriter(options.indent());

        Method method = ArrayEncoder.class.getDeclaredMethod(
            "encodeArrayOfArraysAsListItems",
            String.class,
            ArrayNode.class,
            LineWriter.class,
            int.class,
            EncodeOptions.class
        );
        method.setAccessible(true);

        // When
        method.invoke(null, "items", outer, writer, 0, options);

        // Then
        String expected = String.join("\n",
                                      "items[3]:",
                                      "  - [2]: 1,2"
        );
        assertEquals(expected, writer.toString());
    }

    @Test
    void isArrayOfArraysTestWithObjectNode() {
        // Given
        ObjectNode dataTable = MAPPER.createObjectNode();

        // When
        boolean arrayOfArrays = ArrayEncoder.isArrayOfArrays(dataTable);

        // Then
        assertFalse(arrayOfArrays);
    }

    @Test
    void isArrayOfObjectsTestWithObjectNode() {
        // Given
        ObjectNode dataTable = MAPPER.createObjectNode();

        // When
        boolean arrayOfArrays = ArrayEncoder.isArrayOfObjects(dataTable);

        // Then
        assertFalse(arrayOfArrays);
    }

    @Test
    void encodeArrayWithAllPrimitives() {
        // Given
        ArrayNode arrayNode = jsonNodeFactory.arrayNode();
        arrayNode.add(1).add(2).add(3);
        EncodeOptions options = EncodeOptions.DEFAULT;
        LineWriter lineWriter = new LineWriter(options.indent());

        // When
        ArrayEncoder.encodeArray("", arrayNode, lineWriter, 1, options);

        // Then
        assertFalse(lineWriter.toString().isBlank());
        assertEquals("  \"\"[3]: 1,2,3", lineWriter.toString());
    }

    @Test
    void encodeArrayWithAllPrimitivesArrayOfArrays() {
        // Given
        ArrayNode arrayNode = jsonNodeFactory.arrayNode();
        ArrayNode innerArrayNode = jsonNodeFactory.arrayNode();
        innerArrayNode.add(1).add(2).add(3);
        ArrayNode innerArrayNode2 = jsonNodeFactory.arrayNode();
        innerArrayNode2.add(4).add(5).add(6);

        arrayNode.add(innerArrayNode).add(innerArrayNode2);

        EncodeOptions options = EncodeOptions.DEFAULT;
        LineWriter lineWriter = new LineWriter(options.indent());

        // When
        ArrayEncoder.encodeArray("", arrayNode, lineWriter, 1, options);

        // Then
        assertFalse(lineWriter.toString().isBlank());
        assertEquals("  \"\"[2]:\n" +
                         "    - [3]: 1,2,3\n" +
                         "    - [3]: 4,5,6", lineWriter.toString());
    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<ArrayEncoder> constructor = ArrayEncoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When
        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        // Then
        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }
}
