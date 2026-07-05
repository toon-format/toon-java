package dev.toonformat.jtoon.encoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.EncodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ArrayEncoderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

    @Test
    void isArrayOfPrimitivesTestWithObjectNode() {
        // Given
        final ObjectNode dataTable = MAPPER.createObjectNode();

        // When
        final boolean arrayOfArrays = ArrayEncoder.isArrayOfPrimitives(dataTable);

        // Then
        assertFalse(arrayOfArrays);
    }

    @Test
    @DisplayName("given array-of-arrays with mixed inner types when encodeArrayOfArraysAsListItems"
            + " then writes header and primitive list items")
    void givenArrayOfArraysWithMixedInnerTypes_whenEncodePrivate_thenWritesExpected() throws Exception {
        // Given
        final ArrayNode outer = jsonNodeFactory.arrayNode();

        final ArrayNode innerPrims = jsonNodeFactory.arrayNode().add(1).add(2);
        final ArrayNode innerObjects = jsonNodeFactory.arrayNode();
        innerObjects.add(jsonNodeFactory.objectNode().put("a", 1));
        outer.add(innerPrims).add(innerObjects).add("x");

        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(options.indent());

        final Method method = ArrayEncoder.class.getDeclaredMethod(
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
        final String expected = """
                items[3]:
                  - [2]: 1,2""";
        assertEquals(expected, writer.toString());
    }

    @Test
    void isArrayOfArraysTestWithObjectNode() {
        // Given
        final ObjectNode dataTable = MAPPER.createObjectNode();

        // When
        final boolean arrayOfArrays = ArrayEncoder.isArrayOfArrays(dataTable);

        // Then
        assertFalse(arrayOfArrays);
    }

    @Test
    void isArrayOfObjectsTestWithObjectNode() {
        // Given
        final ObjectNode dataTable = MAPPER.createObjectNode();

        // When
        final boolean arrayOfArrays = ArrayEncoder.isArrayOfObjects(dataTable);

        // Then
        assertFalse(arrayOfArrays);
    }

    @Test
    void encodeArrayWithAllPrimitives() {
        // Given
        final int thirdValue = 3;
        final ArrayNode arrayNode = jsonNodeFactory.arrayNode();
        arrayNode.add(1).add(2).add(thirdValue);
        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter lineWriter = new LineWriter(options.indent());

        // When
        ArrayEncoder.encodeArray("", arrayNode, lineWriter, 1, options);

        // Then
        assertFalse(lineWriter.toString().isBlank());
        assertEquals("  \"\"[3]: 1,2,3", lineWriter.toString());
    }

    @Test
    void encodeArrayWithAllPrimitivesArrayOfArrays() {
        // Given
        final int arr1val3 = 3;
        final int arr2val1 = 4;
        final int arr2val2 = 5;
        final int arr2val3 = 6;
        final ArrayNode arrayNode = jsonNodeFactory.arrayNode();
        final ArrayNode innerArrayNode = jsonNodeFactory.arrayNode();
        innerArrayNode.add(1).add(2).add(arr1val3);
        final ArrayNode innerArrayNode2 = jsonNodeFactory.arrayNode();
        innerArrayNode2.add(arr2val1).add(arr2val2).add(arr2val3);

        arrayNode.add(innerArrayNode).add(innerArrayNode2);

        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter lineWriter = new LineWriter(options.indent());

        // When
        ArrayEncoder.encodeArray("", arrayNode, lineWriter, 1, options);

        // Then
        assertFalse(lineWriter.toString().isBlank());
        @SuppressWarnings("StringConcatToTextBlock")
        final String expected = "  \"\"[2]:\n"
            + "    - [3]: 1,2,3\n"
            + "    - [3]: 4,5,6";
        assertEquals(expected, lineWriter.toString());
    }

    @Test
    @DisplayName("should encode empty keyed array as key: [] without lengthMarker")
    void encodeEmptyArrayAsKeyValue() {
        // Given
        final ArrayNode emptyArray = jsonNodeFactory.arrayNode();
        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(2);

        // When
        ArrayEncoder.encodeArray("tags", emptyArray, writer, 0, options);

        // Then
        assertEquals("tags: []", writer.toString());
    }

    @Test
    @DisplayName("should encode empty keyed array with lengthMarker as header form")
    void encodeEmptyArrayWithLengthMarker() {
        // Given
        final ArrayNode emptyArray = jsonNodeFactory.arrayNode();
        final EncodeOptions options = EncodeOptions.withLengthMarker(true);
        final LineWriter writer = new LineWriter(2);

        // When
        ArrayEncoder.encodeArray("tags", emptyArray, writer, 0, options);

        // Then
        assertEquals("tags[#0]:", writer.toString());
    }

    @Test
    @DisplayName("should encode top-level empty array as [] without lengthMarker")
    void encodeRootEmptyArray() {
        // Given
        final ArrayNode emptyArray = jsonNodeFactory.arrayNode();
        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(2);

        // When
        ArrayEncoder.encodeArray(null, emptyArray, writer, 0, options);

        // Then
        assertEquals("[]", writer.toString());
    }

    @Test
    @DisplayName("should encode top-level empty array with lengthMarker as [0]:")
    void encodeRootEmptyArrayWithLengthMarker() {
        // Given
        final ArrayNode emptyArray = jsonNodeFactory.arrayNode();
        final EncodeOptions options = EncodeOptions.withLengthMarker(true);
        final LineWriter writer = new LineWriter(2);

        // When
        ArrayEncoder.encodeArray(null, emptyArray, writer, 0, options);

        // Then
        assertEquals("[0]:", writer.toString());
    }

    @Test
    @DisplayName("should encode empty nested array as key: []")
    void encodeEmptyNestedArray() {
        // Given
        final ArrayNode emptyArray = jsonNodeFactory.arrayNode();
        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(2);

        // When
        ArrayEncoder.encodeArray("data", emptyArray, writer, 1, options);

        // Then
        assertEquals("  data: []", writer.toString());
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
