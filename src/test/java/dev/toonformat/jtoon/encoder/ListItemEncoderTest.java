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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ListItemEncoderTest {
    private final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
    private final EncodeOptions options = EncodeOptions.DEFAULT;

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<ListItemEncoder> constructor = ListItemEncoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
                assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }

    @Test
    void givenEmptyObject_whenEncoded_thenWritesDashOnly() {
        // Given
        ObjectNode objectNode = jsonNodeFactory.objectNode();
        LineWriter writer = new LineWriter(options.indent());

        // When
        ListItemEncoder.encodeObjectAsListItem(objectNode, writer, 1, options);

        // Then
        assertEquals("  -", writer.toString());

    }

    @Test
    void givenPrimitiveValue_whenEncoded_thenWritesInlinePrimitive() {
        // Given
        ObjectNode objectNode = jsonNodeFactory.objectNode();
        objectNode.put("name", "John");

        LineWriter writer = new LineWriter(options.indent());

        // When
        ListItemEncoder.encodeObjectAsListItem(objectNode, writer, 0, options);

        // Then
        assertEquals("- name: John", writer.toString());
    }

    @Test
    void givenArrayOfPrimitives_whenEncoded_thenWritesInlineArray() {
        // Given
        ObjectNode objectNode = jsonNodeFactory.objectNode();
        ArrayNode arrayNode = jsonNodeFactory.arrayNode().add(1).add(2).add(3);
        objectNode.set("nums", arrayNode);

        LineWriter writer = new LineWriter(options.indent());

        // When
        ListItemEncoder.encodeObjectAsListItem(objectNode, writer, 0, options);

        // Then
        assertEquals("- nums[3]: 1,2,3", writer.toString());
    }

    @Test
    void givenObjectValue_whenEncoded_thenWritesNestedObject() {
        // Given
        ObjectNode objectNode = jsonNodeFactory.objectNode();
        ObjectNode child = jsonNodeFactory.objectNode();
        child.put("age", 30);
        objectNode.set("person", child);

        LineWriter writer = new LineWriter(options.indent());


        // When
        ListItemEncoder.encodeObjectAsListItem(objectNode, writer, 1, options);

        // Then
        assertEquals("  - person:\n" +
                "      age: 30", writer.toString());
    }

    @Test
    void givenMultipleFields_whenEncoded_thenRemainingFieldsAreDelegated() {
        // Given
        ObjectNode objectNode = jsonNodeFactory.objectNode();
        objectNode.put("a", 1);
        objectNode.put("b", 2);

        LineWriter writer = new LineWriter(options.indent());


        // When
        ListItemEncoder.encodeObjectAsListItem(objectNode, writer, 0, options);

        // Then
        assertEquals("- a: 1\n" +
                "  b: 2", writer.toString());
    }

    @Test
    void usesTabularFormatForNestedUniformObjectArrays() {
        // Given
        String json = "[\n" +
            "          { \"users\": [{ \"id\": 1, \"name\": \"Ada\" }, { \"id\": 2, \"name\": \"Bob\" }], \"status\": \"active\" }\n" +
            "        ]";
        ArrayNode node = (ArrayNode) new ObjectMapper().readTree(json);

        EncodeOptions options = EncodeOptions.DEFAULT;
        LineWriter writer = new LineWriter(options.indent());
        Set<String> rootKeys = new HashSet<>();

        // When
        ArrayEncoder.encodeArray("items",node, writer, 0, options);

        // Then
        String expected = String.join("\n",
                                      "items[1]:",
                                      "  - users[2]{id,name}:",
                                      "      1,Ada",
                                      "      2,Bob",
                                      "    status: active");
        assertEquals(expected, writer.toString());
    }

    @Test
    void usesListFormatForNestedObjectArraysWithMismatchedKeys() {
        // Given
        String json = "[\n" +
                "          { \"users\": [{ \"id\": 1, \"name\": \"Ada\" }, { \"id\": 2 }], \"status\": \"active\" }\n" +
                "        ]";
        ArrayNode node = (ArrayNode) new ObjectMapper().readTree(json);

        EncodeOptions options = EncodeOptions.DEFAULT;
        LineWriter writer = new LineWriter(options.indent());
        Set<String> rootKeys = new HashSet<>();

        // When
        ArrayEncoder.encodeArray("items", node, writer, 0, options);


        // Then
        String expected = String.join("\n",
                                      "items[1]:",
                                      "  - users[2]:",
                                      "      - id: 1",
                                      "        name: Ada",
                                      "      - id: 2",
                                      "    status: active");
        assertEquals(expected, writer.toString());
    }
}