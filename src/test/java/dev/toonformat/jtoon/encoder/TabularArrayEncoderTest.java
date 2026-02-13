package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TabularArrayEncoderTest {

    private static final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
    private static final EncodeOptions options = EncodeOptions.DEFAULT;

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<TabularArrayEncoder> constructor = TabularArrayEncoder.class.getDeclaredConstructor();
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
    void givenEmptyArray_whenDetectHeader_thenReturnsEmpty() {
        // Given
        ArrayNode rows = jsonNodeFactory.arrayNode();

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenFirstRowNotObject_whenDetectHeader_thenReturnsEmpty() {
        // Given
        ArrayNode rows = jsonNodeFactory.arrayNode().add(1).add(2);

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenFirstObjectHasNoKeys_whenDetectHeader_thenReturnsEmpty() {
        // Given
        ArrayNode rows = jsonNodeFactory.arrayNode();
        rows.add(jsonNodeFactory.objectNode()); // empty object

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenMismatchedKeyCount_whenDetectHeader_thenReturnsEmpty() {
        // Given
        ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2); // missing name key

        ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenMissingHeaderKeyInLaterRow_whenDetectHeader_thenReturnsEmpty() {
        // Given
        ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2);
        b.put("age", 42); // same size but different key set (name missing)

        ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenNonPrimitiveValue_whenDetectHeader_thenReturnsEmpty() {
        // Given
        ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2);
        b.set("name", jsonNodeFactory.objectNode()); // not a primitive

        ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenUniformObjectsDifferentOrder_whenDetectHeader_thenReturnsHeaderKeys() {
        // Given
        ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        ObjectNode b = jsonNodeFactory.objectNode();
        b.put("name", "Bob"); // order swapped
        b.put("id", 2);

        ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertEquals(List.of("id", "name"), header);
    }

    @Test
    void givenUniformObjects_whenEncodeArrayAsTabular_thenWritesHeaderAndRows() {
        // Given
        ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2);
        b.put("name", "Bob");

        ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);
        LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.encodeArrayOfObjectsAsTabular("users", rows, header, writer, 0, options);

        // Then
        String expected = String.join("\n",
                "users[2]{id,name}:",
                "  1,Ada",
                "  2,Bob");
        assertEquals(expected, writer.toString());
    }

    @Test
    void givenHeaderAndRows_whenWriteTabularRows_thenWritesValuesWithIndent() {
        // Given
        ObjectNode a = jsonNodeFactory.objectNode();
        a.put("x", 10);
        a.put("y", 20);

        ObjectNode b = jsonNodeFactory.objectNode();
        b.put("x", 11);
        b.put("y", 21);

        ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        List<String> header = List.of("x", "y");
        LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.writeTabularRows(rows, header, writer, 2, options);

        // Then
        String expected = String.join("\n",
                "    10,20",
                "    11,21");
        assertEquals(expected, writer.toString());
    }

    @Test
    void testDetectTabularHeaderWithEmptyRow() {
        // Given
        ArrayNode rows = jsonNodeFactory.arrayNode();

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithNoneObjectAsFirstItem() {
        // Given
        ArrayNode rows = jsonNodeFactory.arrayNode();
        rows.add(1);

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithEmptyObject() {
        // Given
        ArrayNode rows = jsonNodeFactory.arrayNode();
        ObjectNode a = jsonNodeFactory.objectNode();
        rows.add(a);

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithSecondItemIsNotAnObject() {
        // Given
        ArrayNode rows = jsonNodeFactory.arrayNode();
        ObjectNode a = jsonNodeFactory.objectNode();
        rows.add(a).add(1);

        // When
        List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithUnevenObjectInTheList() {
        // Given
        ObjectNode a = jsonNodeFactory.objectNode();
        a.put("x", 10);
        a.put("y", 20);

        ObjectNode b = jsonNodeFactory.objectNode();
        b.put("x", 11);

        ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        List<String> header = List.of("x", "y");
        LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.writeTabularRows(rows, header, writer, 2, options);

        // Then
        String expected = String.join("\n",
                                      "    10,20",
                                      "    11");
        assertEquals(expected, writer.toString());
    }

    @Test
    void testDetectTabularHeaderWithUnevenObjectArrayMixInTheList() {
        // Given
        ObjectNode a = jsonNodeFactory.objectNode();
        a.put("x", 10);
        a.put("y", 20);

        ArrayNode b = jsonNodeFactory.arrayNode();
        b.add(11);
        b.add(12);

        ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        List<String> header = List.of("x", "y");
        LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.writeTabularRows(rows, header, writer, 2, options);

        // Then
        String expected = String.join("\n",
                                      "    10,20");
        assertEquals(expected, writer.toString());
    }
}
