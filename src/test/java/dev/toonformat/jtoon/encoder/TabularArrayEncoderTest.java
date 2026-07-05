package dev.toonformat.jtoon.encoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import dev.toonformat.jtoon.EncodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

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
        final ArrayNode rows = jsonNodeFactory.arrayNode();

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenFirstRowNotObject_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode().add(1).add(2);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenFirstObjectHasNoKeys_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();
        rows.add(jsonNodeFactory.objectNode()); // empty object

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenMismatchedKeyCount_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2); // missing name key

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenMissingHeaderKeyInLaterRow_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final int extraFieldValue = 42;
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2);
        b.put("age", extraFieldValue); // same size but different key set (name missing)

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenNonPrimitiveValue_whenDetectHeader_thenReturnsEmpty() {
        // Given
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2);
        b.set("name", jsonNodeFactory.objectNode()); // not a primitive

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void givenUniformObjectsDifferentOrder_whenDetectHeader_thenReturnsHeaderKeys() {
        // Given
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("name", "Bob"); // order swapped
        b.put("id", 2);

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertEquals(List.of("id", "name"), header);
    }

    @Test
    void givenUniformObjects_whenEncodeArrayAsTabular_thenWritesHeaderAndRows() {
        // Given
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2);
        b.put("name", "Bob");

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);
        final LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.encodeArrayOfObjectsAsTabular("users", rows, header, writer, 0, options);

        // Then
        final String expected = """
                users[2]{id,name}:
                  1,Ada
                  2,Bob""";
        assertEquals(expected, writer.toString());
    }

    @Test
    void givenHeaderAndRows_whenWriteTabularRows_thenWritesValuesWithIndent() {
        // Given
        final int nodeAx = 10;
        final int nodeAy = 20;
        final int nodeBx = 11;
        final int nodeBy = 21;
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("x", nodeAx);
        a.put("y", nodeAy);

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("x", nodeBx);
        b.put("y", nodeBy);

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        final List<String> header = List.of("x", "y");
        final LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.writeTabularRows(rows, header, writer, 2, options);

        // Then
        @SuppressWarnings("StringConcatToTextBlock")
        final String expected = "    10,20\n"
            + "    11,21";
        assertEquals(expected, writer.toString());
    }

    @Test
    void givenUniformObjects_whenDetectHeader_thenReturnsUnmodifiableList() {
        // Given
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("id", 1);
        a.put("name", "Ada");

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("id", 2);
        b.put("name", "Bob");

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertThrows(UnsupportedOperationException.class, () -> header.add("extra"));
    }

    @Test
    void testDetectTabularHeaderWithEmptyRow() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithNoneObjectAsFirstItem() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();
        rows.add(1);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithEmptyObject() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();
        final ObjectNode a = jsonNodeFactory.objectNode();
        rows.add(a);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithSecondItemIsNotAnObject() {
        // Given
        final ArrayNode rows = jsonNodeFactory.arrayNode();
        final ObjectNode a = jsonNodeFactory.objectNode();
        rows.add(a).add(1);

        // When
        final List<String> header = TabularArrayEncoder.detectTabularHeader(rows);

        // Then
        assertTrue(header.isEmpty());
    }

    @Test
    void testDetectTabularHeaderWithUnevenObjectInTheList() {
        // Given
        final int objAx = 10;
        final int objAy = 20;
        final int objBx = 11;
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("x", objAx);
        a.put("y", objAy);

        final ObjectNode b = jsonNodeFactory.objectNode();
        b.put("x", objBx);

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        final List<String> header = List.of("x", "y");
        final LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.writeTabularRows(rows, header, writer, 2, options);

        // Then
        @SuppressWarnings("StringConcatToTextBlock")
        final String expected = "    10,20\n"
            + "    11";
        assertEquals(expected, writer.toString());
    }

    @Test
    void testDetectTabularHeaderWithUnevenObjectArrayMixInTheList() {
        // Given
        final int mixObjX = 10;
        final int mixObjY = 20;
        final int mixArr1 = 11;
        final int mixArr2 = 12;
        final ObjectNode a = jsonNodeFactory.objectNode();
        a.put("x", mixObjX);
        a.put("y", mixObjY);

        final ArrayNode b = jsonNodeFactory.arrayNode();
        b.add(mixArr1);
        b.add(mixArr2);

        final ArrayNode rows = jsonNodeFactory.arrayNode().add(a).add(b);
        final List<String> header = List.of("x", "y");
        final LineWriter writer = new LineWriter(options.indent());

        // When
        TabularArrayEncoder.writeTabularRows(rows, header, writer, 2, options);

        // Then
        final String expected = "    10,20";
        assertEquals(expected, writer.toString());
    }
}
