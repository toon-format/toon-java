package dev.toonformat.jtoon.encoder;

import dev.toonformat.jtoon.EncodeOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the ObjectEncoder
 */
class ObjectEncoderTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void givenSimpleObject_whenEncoding_thenOutputsCorrectLines() {
        // Given
        ObjectNode objectNode = MAPPER.createObjectNode();
        objectNode.put("x", 10);

        EncodeOptions options = EncodeOptions.DEFAULT;
        LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(objectNode, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("x: 10", writer.toString());
    }

    @Test
    void givenNestedObjectAndFlattenOff_whenEncoding_thenWritesIndentedBlocks() {
        // Given
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode child = MAPPER.createObjectNode();
        child.put("y", "ok");
        root.set("x", child);

        EncodeOptions options = EncodeOptions.DEFAULT;
        LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(root, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("""
                x:
                  y: ok""", writer.toString());
    }

    @Test
    void givenNestedObjectAndFlattenOn_whenSimpleFoldPossible_thenKeyIsFolded() {
        // Given
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode child = MAPPER.createObjectNode();
        child.put("y", 5);
        root.set("x", child);
        EncodeOptions options = EncodeOptions.withFlatten(true);
        LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(root, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("x.y: 5", writer.toString());
    }

    @Test
    void givenPartiallyFoldableKeyChain_whenRemainingDepthTooSmall_thenFlattenStops() {
        // Given
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode a = MAPPER.createObjectNode();
        ObjectNode b = MAPPER.createObjectNode();
        b.put("z", 1);
        a.set("b", b);
        root.set("a", a);

        EncodeOptions options = EncodeOptions.withFlattenDepth(1);
        LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(root, writer, 0, options, new HashSet<>(), null, 0, new HashSet<>());

        // Then → no flattening due to remainingDepth=0
        assertEquals("""
                a:
                  b:
                    z: 1""", writer.toString());
    }

    @Test
    void givenObjectWithLiteralDotsInRoot_whenEncoding_thenRootLiteralKeysAreCollected() {
        // Given
        ObjectNode obj = MAPPER.createObjectNode();
        obj.put("a.b", 1);
        obj.put("c", 2);

        Set<String> rootLiteralKeys = new HashSet<>();
        EncodeOptions options = EncodeOptions.DEFAULT;
        LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(obj, writer, 0, options, rootLiteralKeys, null, null, new HashSet<>());

        // Then
        assertTrue(rootLiteralKeys.contains("a.b"));
        assertFalse(rootLiteralKeys.contains("c"));
    }

    @Test
    void givenArray_whenEncoding_thenDelegatesToArrayEncoder() {
        // Given
        ObjectNode objectNode = MAPPER.createObjectNode();
        ArrayNode arrayNode = MAPPER.createArrayNode();
        arrayNode.add("a");
        arrayNode.add("b");
        objectNode.set("items", arrayNode);

        EncodeOptions options = EncodeOptions.DEFAULT;
        LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(objectNode, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("items[2]: a,b", writer.toString());
    }

    @Test
    void givenEmptyObject_whenEncoding_thenWritesKeyOnly() {
        // Given
        ObjectNode obj = MAPPER.createObjectNode();
        ObjectNode empty = MAPPER.createObjectNode();
        obj.set("x", empty);

        EncodeOptions options = EncodeOptions.DEFAULT;
        LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(obj, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("x:", writer.toString());
    }

    @Test
    void givenMultiLevelFoldChain_whenFullyFoldable_thenEncodesFullyFlattenedKey() {
        // Given (x → { y → { z: 3 }} )
        ObjectNode z = MAPPER.createObjectNode();
        z.put("z", 3);

        ObjectNode y = MAPPER.createObjectNode();
        y.set("y", z);

        ObjectNode x = MAPPER.createObjectNode();
        x.set("x", y);

        EncodeOptions options = EncodeOptions.withFlatten(true);
        LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(x, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("x.y.z: 3", writer.toString());
    }

    @Test
    void givenPartiallyFoldedKeyChain_whenFoldResultHasRemainder_thenEncodesCase2Path() {
        // Given
        String json = "{\"items\": [\"summary\", { \"id\": 1, \"name\": \"Ada\" }, [{ \"id\": 2 }, { \"status\": \"draft\" }]]}";
        ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        EncodeOptions options = EncodeOptions.withFlatten(true);
        LineWriter writer = new LineWriter(options.indent());
        Set<String> rootKeys = new HashSet<>();


        // When
        ObjectEncoder.encodeObject(node, writer, 0, options, rootKeys, null, null, new HashSet<>());

        // Then
        assertEquals("""
                items[3]:
                  - summary
                  - id: 1
                    name: Ada
                  - [2]:
                    - id: 2
                    - status: draft""", writer.toString());
    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<ObjectEncoder> constructor = ObjectEncoder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
                assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }
}