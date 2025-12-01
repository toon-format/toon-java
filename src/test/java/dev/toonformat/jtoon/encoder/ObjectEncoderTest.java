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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the ObjectEncoder
 */
class ObjectEncoderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

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
    @DisplayName("given fully-folded primitive leaf when flatten then writes inline value and returns null")
    void givenFullyFoldedPrimitiveLeaf_whenFlatten_thenWritesInlineAndReturnsNull() throws Exception {
        // Given
        LineWriter writer = new LineWriter(EncodeOptions.DEFAULT.indent());
        EncodeOptions options = EncodeOptions.DEFAULT;
        Set<String> blockedKeys = new HashSet<>();
        String key = "a";

        Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "a.b",
            null,
            new ObjectMapper().readTree("42"),
            2
        );

        Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
            "flatten",
            String.class,
            Flatten.FoldResult.class,
            LineWriter.class,
            int.class,
            EncodeOptions.class,
            Set.class,
            String.class,
            Set.class,
            int.class
        );
        flattenMethod.setAccessible(true);

        // When
        Object result = flattenMethod.invoke(
            null,
            key,
            foldResult,
            writer,
            0,
            options,
            null,
            null,
            blockedKeys,
            5
        );

        // Then
        assertNull(result);
        assertEquals("a.b: 42", writer.toString());
        assertTrue(blockedKeys.contains("a"));
        assertTrue(blockedKeys.contains("a.b"));
    }

    @Test
    @DisplayName("given fully-folded array leaf when flatten then delegates to ArrayEncoder and returns null")
    void givenFullyFoldedArrayLeaf_whenFlatten_thenWritesArrayAndReturnsNull() throws Exception {
        // Given
        LineWriter writer = new LineWriter(EncodeOptions.DEFAULT.indent());
        EncodeOptions options = EncodeOptions.DEFAULT;
        Set<String> blockedKeys = new HashSet<>();
        String key = "items";

        ArrayNode arrayLeaf = (ArrayNode) new ObjectMapper().readTree("[1,2]");
        Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "items.values",
            null,
            arrayLeaf,
            2
        );

        Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
            "flatten",
            String.class,
            Flatten.FoldResult.class,
            LineWriter.class,
            int.class,
            EncodeOptions.class,
            Set.class,
            String.class,
            Set.class,
            int.class
        );
        flattenMethod.setAccessible(true);

        // When
        Object result = flattenMethod.invoke(
            null,
            key,
            foldResult,
            writer,
            0,
            options,
            null,
            null,
            blockedKeys,
            5
        );

        // Then
        assertNull(result);
        assertEquals("items.values[2]: 1,2", writer.toString());
        assertTrue(blockedKeys.contains("items"));
        assertTrue(blockedKeys.contains("items.values"));
    }

    @Test
    @DisplayName("given fully-folded object leaf when flatten then writes header and nested object and returns null")
    void givenFullyFoldedObjectLeaf_whenFlatten_thenWritesObjectAndReturnsNull() throws Exception {
        // Given
        LineWriter writer = new LineWriter(EncodeOptions.DEFAULT.indent());
        EncodeOptions options = EncodeOptions.DEFAULT;
        Set<String> blockedKeys = new HashSet<>();
        String key = "user";

        ObjectNode objectLeaf = (ObjectNode) new ObjectMapper().readTree("{\"id\":1}");
        Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "user.info",
            null,
            objectLeaf,
            2
        );

        Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
            "flatten",
            String.class,
            Flatten.FoldResult.class,
            LineWriter.class,
            int.class,
            EncodeOptions.class,
            Set.class,
            String.class,
            Set.class,
            int.class
        );
        flattenMethod.setAccessible(true);

        // When
        Object result = flattenMethod.invoke(
            null,
            key,
            foldResult,
            writer,
            0,
            options,
            null,
            null,
            blockedKeys,
            5
        );

        // Then
        assertNull(result);
        String expected = String.join("\n",
                                      "user.info:",
                                      "  id: 1"
        );
        assertEquals(expected, writer.toString());
        assertTrue(blockedKeys.contains("user"));
        assertTrue(blockedKeys.contains("user.info"));
    }

    @Test
    @DisplayName("given non-object remainder when flatten then returns options (not null) and writes nothing")
    void givenNonObjectRemainder_whenFlatten_thenReturnsOptionsNotNullAndNoOutput() throws Exception {
        // Given
        LineWriter writer = new LineWriter(EncodeOptions.DEFAULT.indent());
        EncodeOptions options = EncodeOptions.DEFAULT;
        Set<String> blockedKeys = new HashSet<>();
        String key = "cfg";

        ArrayNode remainderArray = (ArrayNode) new ObjectMapper().readTree("[1]");
        Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "cfg.path",
            remainderArray,
            null,
            2
        );

        Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
            "flatten",
            String.class,
            Flatten.FoldResult.class,
            LineWriter.class,
            int.class,
            EncodeOptions.class,
            Set.class,
            String.class,
            Set.class,
            int.class
        );
        flattenMethod.setAccessible(true);

        // When
        Object result = flattenMethod.invoke(
            null,
            key,
            foldResult,
            writer,
            0,
            options,
            null,
            null,
            blockedKeys,
            5
        );

        // Then
        assertNotNull(result, "flatten should not always return null");
        assertSame(options, result, "Expected the same options instance to be returned");
        assertEquals("", writer.toString(), "No output should be produced for non-object remainder");
        assertTrue(blockedKeys.contains("cfg"));
        assertTrue(blockedKeys.contains("cfg.path"));
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

        // Then
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
        // Given
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

    @Test
    void givenPrimitiveLeaf_whenFlatten_thenWriterReceivesEncodedLine() throws Exception {
        // given
        String key = "a";
        EncodeOptions options = EncodeOptions.withFlatten(true);
        LineWriter writer = new LineWriter(options.indent());

        Set<String> rootLiteralKeys = new HashSet<>();
        Set<String> blockedKeys = new HashSet<>();
        Flatten.FoldResult fullFold = new Flatten.FoldResult(
            "a.b",   // foldedKey
            null,    // remainder
            MAPPER.readTree("1"), // leafValue
            1        // segmentCount
        );

        // Access private method
        Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
            "flatten",
            String.class,
            Flatten.FoldResult.class,
            LineWriter.class,
            int.class,
            EncodeOptions.class,
            Set.class,
            String.class,
            Set.class,
            int.class
        );
        flattenMethod.setAccessible(true);

        // when
        Object returnValue = flattenMethod.invoke(
            null,  // static method
            key,
            fullFold,
            writer,
            0,
            options,
            rootLiteralKeys,
            null,
            blockedKeys,
            5
        );

        // then
        assertNull(returnValue, "Expected null for fully folded primitive case");
        assertEquals(1, writer.toString().lines().count(), "Writer should contain one line");

        String line = writer.toString();
        assertEquals("a.b: 1", line);

        assertEquals(2, blockedKeys.size());
        assertTrue(blockedKeys.contains("a"));
    }

    @Test
    void givenPartiallyFolded_whenFlatten_thenWriterReceivesFoldedKeyAndObjectIsEncoded() throws Exception {
        // given
        String key = "a";

        EncodeOptions options = EncodeOptions.withFlattenDepth(5);
        LineWriter writer = new LineWriter(options.indent());

        Set<String> rootLiteralKeys = new HashSet<>();
        Set<String> blockedKeys = new HashSet<>();

        ObjectNode remainderNode = (ObjectNode) MAPPER.readTree("{\"c\": 5}");

        Flatten.FoldResult partialFold = new Flatten.FoldResult(
            "a.b",
            remainderNode,
            null,
            1
        );

        // Access private method
        Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
            "flatten",
            String.class,
            Flatten.FoldResult.class,
            LineWriter.class,
            int.class,
            EncodeOptions.class,
            Set.class,
            String.class,
            Set.class,
            int.class
        );
        flattenMethod.setAccessible(true);

        // when
        Object result = flattenMethod.invoke(
            null,               // static
            key,                // "a"
            partialFold,          // {"b":{"c":5}}
            writer,
            0,                  // depth
            options,
            rootLiteralKeys,
            null,               // pathPrefix
            blockedKeys,
            1                   // remainingDepth (will go to <=0, disable flattening)
        );

        // then
        assertNull(result);
        assertEquals(2, writer.toString().lines().count(), "Writer should contain two lines");

        assertTrue(blockedKeys.contains("a"), "Original key should be blocked");
        assertTrue(blockedKeys.contains("a.b"), "Folded key should be blocked");
    }

    @Test
    void usesListFormatForObjectsContainingArraysOfArrays() {
        // Given
        String json = "{\n" +
            "        \"items\": [\n" +
            "          { \"matrix\": [[1, 2], [3, 4]], \"name\": \"grid\" }\n" +
            "        ]\n" +
            "      }";
        ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        EncodeOptions options = EncodeOptions.withFlatten(true);
        LineWriter writer = new LineWriter(options.indent());
        Set<String> rootKeys = new HashSet<>();

        // When
        ObjectEncoder.encodeObject(node, writer, 0, options, rootKeys, null, null, new HashSet<>());

        // Then
        String expected = String.join("\n",
                                      "items[1]:",
                                      "  - matrix[2]:",
                                      "      - [2]: 1,2",
                                      "      - [2]: 3,4",
                                      "    name: grid");
        assertEquals(expected, writer.toString());
    }

    @Test
    void testEncodeKeyValuePairWithAKey() {
        // Given
        String json = "{\n" +
            "        \"items\": [\n" +
            "          { \"matrix\": [[1, 2], [3, 4]], \"name\": \"grid\" }\n" +
            "        ]\n" +
            "      }";
        ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        EncodeOptions options = EncodeOptions.withFlatten(true);
        LineWriter writer = new LineWriter(options.indent());
        Set<String> rootKeys = new HashSet<>();

        // When
        ObjectEncoder.encodeKeyValuePair("items", node, writer, 0, options, rootKeys, null, null, 10, new HashSet<>());

        // Then
        String expected = String.join("\n",
                                      "items:",
                                      "  items[1]:",
                                      "    - matrix[2]:",
                                      "        - [2]: 1,2",
                                      "        - [2]: 3,4",
                                      "      name: grid");
        assertEquals(expected, writer.toString());
    }
    @Test
    void testEncodeKeyValuePairWithNullFlattenDepth() {
        // Given
        String json = "{\n" +
            "        \"items\": [\n" +
            "          { \"matrix\": [[1, 2], [3, 4]], \"name\": \"grid\" }\n" +
            "        ]\n" +
            "      }";
        ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        EncodeOptions options = EncodeOptions.withFlatten(true);
        LineWriter writer = new LineWriter(options.indent());
        Set<String> rootKeys = new HashSet<>();

        // When
        ObjectEncoder.encodeKeyValuePair("items", node, writer, 0, options, rootKeys, null, null, null, new HashSet<>());

        // Then
        String expected = String.join("\n",
            "items:",
            "  items[1]:",
            "    - matrix[2]:",
            "        - [2]: 1,2",
            "        - [2]: 3,4",
            "      name: grid");
        assertEquals(expected, writer.toString());
    }
    @Test
    void testEncodeKeyValuePairWithToSmallFlattenDepth() {
        // Given
        String json = "{\n" +
            "        \"items\": [\n" +
            "          { \"matrix\": [[1, 2], [3, 4]], \"name\": \"grid\" }\n" +
            "        ]\n" +
            "      }";
        ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        EncodeOptions options = EncodeOptions.withFlatten(true);
        LineWriter writer = new LineWriter(options.indent());
        Set<String> rootKeys = new HashSet<>();

        // When
        ObjectEncoder.encodeKeyValuePair("items", node, writer, 0, options, rootKeys, null, null, 0, new HashSet<>());

        // Then
        String expected = String.join("\n",
            "items:",
            "  items[1]:",
            "    - matrix[2]:",
            "        - [2]: 1,2",
            "        - [2]: 3,4",
            "      name: grid");
        assertEquals(expected, writer.toString());
    }

    @Test
    void testEncodeKeyValuePairWithoutEmptySiblings() {
        // Given
        ObjectNode node = jsonNodeFactory.objectNode();

        EncodeOptions options = EncodeOptions.withFlatten(true);
        LineWriter writer = new LineWriter(options.indent());
        Set<String> siblings = new HashSet<>();
        siblings.add("hello");
        siblings.add("world");

        // When
        ObjectEncoder.encodeKeyValuePair("", node, writer, 0, options, siblings, null, null, 10, new HashSet<>());

        // Then
        assertFalse(writer.toString().trim().isEmpty());
        //we only get a String with ""
    }

}
