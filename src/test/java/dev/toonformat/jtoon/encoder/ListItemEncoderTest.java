package dev.toonformat.jtoon.encoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import dev.toonformat.jtoon.EncodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ListItemEncoderTest {
    private static final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
    private static final EncodeOptions options = EncodeOptions.DEFAULT;

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<ListItemEncoder> constructor = ListItemEncoder.class.getDeclaredConstructor();
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
    void givenEmptyObject_whenEncoded_thenWritesDashOnly() {
        // Given
        final ObjectNode objectNode = jsonNodeFactory.objectNode();
        final LineWriter writer = new LineWriter(options.indent());

        // When
        ListItemEncoder.encodeObjectAsListItem(objectNode, writer, 1, options);

        // Then
        assertEquals("  -", writer.toString());

    }

    @Test
    void givenPrimitiveValue_whenEncoded_thenWritesInlinePrimitive() {
        // Given
        final ObjectNode objectNode = jsonNodeFactory.objectNode();
        objectNode.put("name", "John");

        final LineWriter writer = new LineWriter(options.indent());

        // When
        ListItemEncoder.encodeObjectAsListItem(objectNode, writer, 0, options);

        // Then
        assertEquals("- name: John", writer.toString());
    }

    @Test
    void givenArrayOfPrimitives_whenEncoded_thenWritesInlineArray() {
        // Given
        final ObjectNode objectNode = jsonNodeFactory.objectNode();
        final ArrayNode arrayNode = jsonNodeFactory.arrayNode().add(1).add(2).add(3);
        objectNode.set("nums", arrayNode);

        final LineWriter writer = new LineWriter(options.indent());

        // When
        ListItemEncoder.encodeObjectAsListItem(objectNode, writer, 0, options);

        // Then
        assertEquals("- nums[3]: 1,2,3", writer.toString());
    }

    @Test
    void givenObjectValue_whenEncoded_thenWritesNestedObject() {
        // Given
        final int testAge = 30;
        final ObjectNode objectNode = jsonNodeFactory.objectNode();
        final ObjectNode child = jsonNodeFactory.objectNode();
        child.put("age", testAge);
        objectNode.set("person", child);

        final LineWriter writer = new LineWriter(options.indent());


        // When
        ListItemEncoder.encodeObjectAsListItem(objectNode, writer, 1, options);

        // Then
        assertEquals("  - person:\n" +
                "      age: 30", writer.toString());
    }

    @Test
    void givenMultipleFields_whenEncoded_thenRemainingFieldsAreDelegated() {
        // Given
        final ObjectNode objectNode = jsonNodeFactory.objectNode();
        objectNode.put("a", 1);
        objectNode.put("b", 2);

        final LineWriter writer = new LineWriter(options.indent());


        // When
        ListItemEncoder.encodeObjectAsListItem(objectNode, writer, 0, options);

        // Then
        assertEquals("- a: 1\n" +
                "  b: 2", writer.toString());
    }

    @Test
    void usesTabularFormatForNestedUniformObjectArrays() {
        // Given
        final String json = "[\n" +
            "          { \"users\": [{ \"id\": 1, \"name\": \"Ada\" },"
                    + " { \"id\": 2, \"name\": \"Bob\" }], \"status\": \"active\" }\n" +
            "        ]";
        final ArrayNode node = (ArrayNode) new ObjectMapper().readTree(json);

        final EncodeOptions opts = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(opts.indent());

        // When
        ArrayEncoder.encodeArray("items",node, writer, 0, opts);

        // Then
        final String expected = """
                items[1]:
                  - users[2]{id,name}:
                      1,Ada
                      2,Bob
                    status: active""";
        assertEquals(expected, writer.toString());
    }

    @Test
    void usesListFormatForNestedObjectArraysWithMismatchedKeys() {
        // Given
        final String json = """
            [
                      { "users": [{ "id": 1, "name": "Ada" }, { "id": 2 }], "status": "active" }
                    ]""";
        final ArrayNode node = (ArrayNode) new ObjectMapper().readTree(json);

        final EncodeOptions opts = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(opts.indent());

        // When
        ArrayEncoder.encodeArray("items", node, writer, 0, opts);


        // Then
        final String expected = """
                items[1]:
                  - users[2]:
                      - id: 1
                        name: Ada
                      - id: 2
                    status: active""";
        assertEquals(expected, writer.toString());
    }

    @Test
    void givenMixedTypeArrayAsFirstValue_whenEncoded_thenWritesComplexListFormat() {
        // Given
        final int firstValue = 10;
        final int secondValue = 11;
        final int nestedValue = 5;
        final ObjectNode obj = jsonNodeFactory.objectNode();
        final ArrayNode mixed = jsonNodeFactory.arrayNode();
        // primitive
        mixed.add(1);
        // array of primitives
        mixed.add(jsonNodeFactory.arrayNode().add(firstValue).add(secondValue));
        // object
        final ObjectNode nested = jsonNodeFactory.objectNode();
        nested.put("a", nestedValue);
        mixed.add(nested);
        obj.set("mixed", mixed);

        final LineWriter writer = new LineWriter(options.indent());

        // When
        ListItemEncoder.encodeObjectAsListItem(obj, writer, 0, options);

        // Then
        final String expected = """
                - mixed[3]:
                    - 1
                    - [2]: 10,11
                    - a: 5""";
        assertEquals(expected, writer.toString());
    }
}
