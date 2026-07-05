package dev.toonformat.jtoon.encoder;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import dev.toonformat.jtoon.EncodeOptions;
import dev.toonformat.jtoon.KeyFolding;
import dev.toonformat.jtoon.decoder.DecodeContext;
import dev.toonformat.jtoon.normalizer.JsonNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ValueNode;

/**
 * Test for the ObjectEncoder.
 */
class ObjectEncoderTest {

    private static final int REMAINING_DEPTH = 10;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

    @Test
    void givenSimpleObject_whenEncoding_thenOutputsCorrectLines() {
        // Given
        final ObjectNode objectNode = MAPPER.createObjectNode();
        final int testValueX = 10;
        objectNode.put("x", testValueX);

        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(objectNode, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("x: 10", writer.toString());
    }

    @Test
    void givenSimpleObject_withNullRootLiteralKeys_whenEncoding_thenOutputsCorrectLines() {
        // Given
        final int testValueX2 = 10;
        final ObjectNode objectNode = MAPPER.createObjectNode();
        objectNode.put("x", testValueX2);

        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(objectNode, writer, 0, options, null, null, null, new HashSet<>());

        // Then
        assertEquals("x: 10", writer.toString());
    }

    @Test
    void givenSimpleObject_whenEncoding_thenOutputsInCorrectLines() {
        // Given
        final int testValueX3 = 10;
        final int initialDepth = 25;
        final ObjectNode objectNode = MAPPER.createObjectNode();
        objectNode.put("x", testValueX3);

        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(objectNode, writer, initialDepth, options,
            new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("                                                  x: 10", writer.toString());
    }

    @Test
    @DisplayName("given fully-folded primitive leaf when flatten then writes inline value and returns null")
    void givenFullyFoldedPrimitiveLeaf_whenFlatten_thenWritesInlineAndReturnsNull() throws Exception {
        // Given
        final LineWriter writer = new LineWriter(EncodeOptions.DEFAULT.indent());
        final EncodeOptions options = EncodeOptions.DEFAULT;
        final Set<String> blockedKeys = new HashSet<>();
        final String key = "a";

        final Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "a.b",
            null,
            new ObjectMapper().readTree("42"),
            2
        );

        final Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
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
        final Object result = flattenMethod.invoke(
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
        final LineWriter writer = new LineWriter(EncodeOptions.DEFAULT.indent());
        final EncodeOptions options = EncodeOptions.DEFAULT;
        final Set<String> blockedKeys = new HashSet<>();
        final String key = "items";

        final ArrayNode arrayLeaf = (ArrayNode) new ObjectMapper().readTree("[1,2]");
        final Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "items.values",
            null,
            arrayLeaf,
            2
        );

        final Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
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
        final Object result = flattenMethod.invoke(
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
        final LineWriter writer = new LineWriter(EncodeOptions.DEFAULT.indent());
        final EncodeOptions options = EncodeOptions.DEFAULT;
        final Set<String> blockedKeys = new HashSet<>();
        final String key = "user";

        final ObjectNode objectLeaf = (ObjectNode) new ObjectMapper().readTree("{\"id\":1}");
        final Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "user.info",
            null,
            objectLeaf,
            2
        );

        final Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
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
        final Object result = flattenMethod.invoke(
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
        final String expected = """
            user.info:
              id: 1""";
        assertEquals(expected, writer.toString());
        assertTrue(blockedKeys.contains("user"));
        assertTrue(blockedKeys.contains("user.info"));
    }

    @Test
    @DisplayName("given non-object remainder when flatten then returns options (not null) and writes nothing")
    void givenNonObjectRemainder_whenFlatten_thenReturnsOptionsNotNullAndNoOutput() throws Exception {
        // Given
        final LineWriter writer = new LineWriter(EncodeOptions.DEFAULT.indent());
        final EncodeOptions options = EncodeOptions.DEFAULT;
        final Set<String> blockedKeys = new HashSet<>();
        final String key = "cfg";

        final ArrayNode remainderArray = (ArrayNode) new ObjectMapper().readTree("[1]");
        final Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "cfg.path",
            remainderArray,
            null,
            2
        );

        final Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
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
        final Object result = flattenMethod.invoke(
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
        final ObjectNode root = MAPPER.createObjectNode();
        final ObjectNode child = MAPPER.createObjectNode();
        child.put("y", "ok");
        root.set("x", child);

        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(options.indent());

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
        final ObjectNode root = MAPPER.createObjectNode();
        final ObjectNode child = MAPPER.createObjectNode();
        final int foldedValue = 5;
        child.put("y", foldedValue);
        root.set("x", child);
        final EncodeOptions options = EncodeOptions.withFlatten(true);
        final LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(root, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("x.y: 5", writer.toString());
    }

    @Test
    void givenPartiallyFoldableKeyChain_whenRemainingDepthTooSmall_thenFlattenStops() {
        // Given
        final ObjectNode root = MAPPER.createObjectNode();
        final ObjectNode a = MAPPER.createObjectNode();
        final ObjectNode b = MAPPER.createObjectNode();
        b.put("z", 1);
        a.set("b", b);
        root.set("a", a);

        final EncodeOptions options = EncodeOptions.withFlattenDepth(1);
        final LineWriter writer = new LineWriter(options.indent());

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
        final ObjectNode obj = MAPPER.createObjectNode();
        obj.put("a.b", 1);
        obj.put("c", 2);

        final Set<String> rootLiteralKeys = new HashSet<>();
        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(obj, writer, 0, options, rootLiteralKeys, null, null, new HashSet<>());

        // Then
        assertTrue(rootLiteralKeys.contains("a.b"));
        assertFalse(rootLiteralKeys.contains("c"));
    }

    @Test
    void givenArray_whenEncoding_thenDelegatesToArrayEncoder() {
        // Given
        final ObjectNode objectNode = MAPPER.createObjectNode();
        final ArrayNode arrayNode = MAPPER.createArrayNode();
        arrayNode.add("a");
        arrayNode.add("b");
        objectNode.set("items", arrayNode);

        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(objectNode, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("items[2]: a,b", writer.toString());
    }

    @Test
    void givenEmptyObject_whenEncoding_thenWritesKeyOnly() {
        // Given
        final ObjectNode obj = MAPPER.createObjectNode();
        final ObjectNode empty = MAPPER.createObjectNode();
        obj.set("x", empty);

        final EncodeOptions options = EncodeOptions.DEFAULT;
        final LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(obj, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("x:", writer.toString());
    }

    @Test
    void givenMultiLevelFoldChain_whenFullyFoldable_thenEncodesFullyFlattenedKey() {
        // Given
        final ObjectNode z = MAPPER.createObjectNode();
        final int multiLevelValue = 3;
        z.put("z", multiLevelValue);

        final ObjectNode y = MAPPER.createObjectNode();
        y.set("y", z);

        final ObjectNode x = MAPPER.createObjectNode();
        x.set("x", y);

        final EncodeOptions options = EncodeOptions.withFlatten(true);
        final LineWriter writer = new LineWriter(options.indent());

        // When
        ObjectEncoder.encodeObject(x, writer, 0, options, new HashSet<>(), null, null, new HashSet<>());

        // Then
        assertEquals("x.y.z: 3", writer.toString());
    }

    @Test
    void givenPartiallyFoldedKeyChain_whenFoldResultHasRemainder_thenEncodesCase2Path() {
        // Given
        final String json = "{\"items\": [\"summary\", { \"id\": 1, \"name\": \"Ada\" },"
                + " [{ \"id\": 2 }, { \"status\": \"draft\" }]]}";
        final ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        final EncodeOptions options = EncodeOptions.withFlatten(true);
        final LineWriter writer = new LineWriter(options.indent());
        final Set<String> rootKeys = new HashSet<>();

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
        // Given
        final Constructor<ObjectEncoder> constructor = ObjectEncoder.class.getDeclaredConstructor();
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
    void givenPrimitiveLeaf_whenFlatten_thenWriterReceivesEncodedLine() throws Exception {
        // Given
        final String key = "a";
        final EncodeOptions options = EncodeOptions.withFlatten(true);
        final LineWriter writer = new LineWriter(options.indent());

        final Set<String> rootLiteralKeys = new HashSet<>();
        final Set<String> blockedKeys = new HashSet<>();
        final Flatten.FoldResult fullFold = new Flatten.FoldResult(
            "a.b",   // foldedKey
            null,    // remainder
            MAPPER.readTree("1"), // leafValue
            1        // segmentCount
        );

        // Access private method
        final Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
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
        final Object returnValue = flattenMethod.invoke(
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

        // Then
        assertNull(returnValue, "Expected null for fully folded primitive case");
        assertEquals(1, writer.toString().lines().count(), "Writer should contain one line");

        final String line = writer.toString();
        assertEquals("a.b: 1", line);

        assertEquals(2, blockedKeys.size());
        assertTrue(blockedKeys.contains("a"));
    }

    @Test
    void givenPartiallyFolded_whenFlatten_thenWriterReceivesFoldedKeyAndObjectIsEncoded() throws Exception {
        // Given
        final String key = "a";

        final EncodeOptions options = EncodeOptions.withFlattenDepth(5);
        final LineWriter writer = new LineWriter(options.indent());

        final Set<String> rootLiteralKeys = new HashSet<>();
        final Set<String> blockedKeys = new HashSet<>();

        final ObjectNode remainderNode = (ObjectNode) MAPPER.readTree("{\"c\": 5}");

        final Flatten.FoldResult partialFold = new Flatten.FoldResult(
            "a.b",
            remainderNode,
            null,
            1
        );

        // Access private method
        final Method flattenMethod = ObjectEncoder.class.getDeclaredMethod(
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
        final Object result = flattenMethod.invoke(
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

        // Then
        assertNull(result);
        assertEquals(2, writer.toString().lines().count(), "Writer should contain two lines");

        assertTrue(blockedKeys.contains("a"), "Original key should be blocked");
        assertTrue(blockedKeys.contains("a.b"), "Folded key should be blocked");
    }

    @Test
    void usesListFormatForObjectsContainingArraysOfArrays() {
        // Given
        final String json = """
        {
                "items": [
                  { "matrix": [[1, 2], [3, 4]], "name": "grid" }
                ]
              }""";
        final ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        final EncodeOptions options = EncodeOptions.withFlatten(true);
        final LineWriter writer = new LineWriter(options.indent());
        final Set<String> siblings = new HashSet<>();

        // When
        ObjectEncoder.encodeObject(node, writer, 0, options, siblings, null, null, new HashSet<>());

        // Then
        final String expected = """
            items[1]:
              - matrix[2]:
                  - [2]: 1,2
                  - [2]: 3,4
                name: grid""";
        assertEquals(expected, writer.toString());
    }

    @Test
    void testEncodeKeyValuePairWithAKey() {
        // Given
        final String json = """
        {
                "items": [
                  { "matrix": [[1, 2], [3, 4]], "name": "grid" }
                ]
              }""";
        final ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        final EncodeOptions options = EncodeOptions.withFlatten(true);
        final LineWriter writer = new LineWriter(options.indent());
        final Set<String> siblings = new HashSet<>();

        // When
        ObjectEncoder.encodeKeyValuePair("items", node, writer, 0, options,
            siblings, null, null, REMAINING_DEPTH, new HashSet<>());

        // Then
        final String expected = """
            items:
              items[1]:
                - matrix[2]:
                    - [2]: 1,2
                    - [2]: 3,4
                  name: grid""";
        assertEquals(expected, writer.toString());
    }

    @Test
    void testEncodeKeyValuePairWithANullKey() {
        // Given
        final String json = """
        {
                "items": [
                  { "matrix": [[1, 2], [3, 4]], "name": "grid" }
                ]
              }""";
        final ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        final EncodeOptions options = EncodeOptions.withFlatten(true);
        final LineWriter writer = new LineWriter(options.indent());
        final Set<String> siblings = new HashSet<>();

        // When
        ObjectEncoder.encodeKeyValuePair(null, node, writer, 0, options,
            siblings, null, null, REMAINING_DEPTH, new HashSet<>());

        // Then
        final String expected = "";
        assertEquals(expected, writer.toString());
    }

    @Test
    void testEncodeKeyValuePairWithNullFlattenDepth() {
        // Given
        final String json = """
        {
                "items": [
                  { "matrix": [[1, 2], [3, 4]], "name": "grid" }
                ]
              }""";
        final ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        final EncodeOptions options = EncodeOptions.withFlatten(true);
        final LineWriter writer = new LineWriter(options.indent());
        final Set<String> siblings = new HashSet<>();

        // When
        ObjectEncoder.encodeKeyValuePair("items", node, writer, 0, options, siblings,
            null, null, null, new HashSet<>());

        // Then
        final String expected = """
            items:
              items[1]:
                - matrix[2]:
                    - [2]: 1,2
                    - [2]: 3,4
                  name: grid""";
        assertEquals(expected, writer.toString());
    }

    @Test
    void testEncodeKeyValuePairWithoutEmptySiblings() {
        // Given
        final ObjectNode node = jsonNodeFactory.objectNode();

        final EncodeOptions options = EncodeOptions.withFlatten(true);
        final LineWriter writer = new LineWriter(options.indent());
        final Set<String> siblings = new HashSet<>();
        siblings.add("hello");
        siblings.add("world");

        // When
        ObjectEncoder.encodeKeyValuePair("items", node, writer, 0, options, siblings,
            null, null, null, new HashSet<>());

        // Then
        assertFalse(writer.toString().trim().isEmpty());
        //we only get a String with ""
    }

    @Test
    void testEncodeKeyValuePairWithKeyInBlockedKeysSet() {
        // Given
        final String json = """
        {
                "items": [
                  { "matrix": [[1, 2], [3, 4]], "name": "grid" }
                ]
              }""";
        final ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        final EncodeOptions options = EncodeOptions.withFlatten(true);
        final LineWriter writer = new LineWriter(options.indent());
        final Set<String> siblings = Set.of("hello", "world");
        final Set<String> blockedKeys = Set.of("items");

        // When
        ObjectEncoder.encodeKeyValuePair("items", node, writer, 0, options,
            siblings, null, null, REMAINING_DEPTH, blockedKeys);

        // Then
        final String expected = """
            items:
              items[1]:
                - matrix[2]:
                    - [2]: 1,2
                    - [2]: 3,4
                  name: grid""";
        assertEquals(expected, writer.toString());
    }

    @Test
    void testEncodeKeyValuePairWithoutFlattenWithAKey() {
        // Given
        final String json = """
        {
                "items": [
                  { "matrix": [[1, 2], [3, 4]], "name": "grid" }
                ]
              }""";
        final ObjectNode node = (ObjectNode) new ObjectMapper().readTree(json);

        final EncodeOptions options = EncodeOptions.withFlatten(false);
        final LineWriter writer = new LineWriter(options.indent());
        final Set<String> siblings = new HashSet<>();

        // When
        ObjectEncoder.encodeKeyValuePair("items", node, writer, 0, options,
            siblings, null, null, REMAINING_DEPTH, new HashSet<>());

        // Then
        final String expected = """
            items:
              items[1]:
                - matrix[2]:
                    - [2]: 1,2
                    - [2]: 3,4
                  name: grid""";
        assertEquals(expected, writer.toString());
    }

    @Test
    void handleFullyFoldedLeafForObjectNodeAsLeaf() throws Exception {
        // Given
        final ObjectNode objectLeaf = (ObjectNode) new ObjectMapper().readTree("{\"id\":1}");
        final Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "user.info",
            null,
            objectLeaf,
            2
        );

        final EncodeOptions options = EncodeOptions.withFlatten(false);
        final LineWriter writer = new LineWriter(options.indent());

        // When
        invokePrivateStatic("handleFullyFoldedLeaf",
            new Class[]{Flatten.FoldResult.class, LineWriter.class, int.class, EncodeOptions.class, String.class},
            foldResult, writer, 2, options, "item");

        // Then
        final String expected = "        item:\n      id: 1";
        assertEquals(expected, writer.toString());
    }

    @Test
    void handleFullyFoldedLeafForBokenNodeAsLeaf() throws Exception {
        // Given
        final ObjectNode objectLeaf = (ObjectNode) new ObjectMapper().readTree("{\"id\":1}");
        final Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "user.info",
            null,
            objectLeaf,
            2
        );

        final EncodeOptions options = EncodeOptions.withFlatten(false);
        final LineWriter writer = new LineWriter(options.indent());

        // When
        invokePrivateStatic("handleFullyFoldedLeaf",
            new Class[]{Flatten.FoldResult.class, LineWriter.class, int.class, EncodeOptions.class, String.class},
            foldResult, writer, 2, options, "item");

        // Then
        final String expected = "        item:\n      id: 1";
        assertEquals(expected, writer.toString());
    }

    @Test
    void testingFlattenWithoutPathPrefix() throws Exception {
        // Given
        final ObjectNode reminder = (ObjectNode) new ObjectMapper().readTree("{\"id\":2}");
        final ObjectNode objectLeaf = (ObjectNode) new ObjectMapper().readTree("{\"id\":1}");
        final Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "user.info",
            reminder,
            objectLeaf,
            2
        );

        final Set<String> blockedKeys = new HashSet<>();

        final EncodeOptions options = EncodeOptions.withFlatten(false);
        final LineWriter writer = new LineWriter(options.indent());

        // When
        final EncodeOptions expectedEncodeOptions = (EncodeOptions) invokePrivateStatic("flatten",
            new Class[]{String.class, Flatten.FoldResult.class, LineWriter.class, int.class,
                EncodeOptions.class, Set.class, String.class, Set.class, int.class},
            "key", foldResult, writer, 2, options, Set.of(), null, blockedKeys, 3);

        // Then
        assertNull(expectedEncodeOptions);
    }
    @Test
    void testingFlattenWithPathPrefix() throws Exception {
        // Given
        final ObjectNode reminder = (ObjectNode) new ObjectMapper().readTree("{\"id\":2}");
        final ObjectNode objectLeaf = (ObjectNode) new ObjectMapper().readTree("{\"id\":1}");
        final Flatten.FoldResult foldResult = new Flatten.FoldResult(
            "user.info",
            reminder,
            objectLeaf,
            2
        );

        final Set<String> blockedKeys = new HashSet<>();

        final EncodeOptions options = EncodeOptions.withFlatten(false);
        final LineWriter writer = new LineWriter(options.indent());

        // When
        final EncodeOptions expectedEncodeOptions = (EncodeOptions) invokePrivateStatic("flatten",
            new Class[]{String.class, Flatten.FoldResult.class, LineWriter.class, int.class,
                EncodeOptions.class, Set.class, String.class, Set.class, int.class},
            "key", foldResult, writer, 2, options, Set.of(), "user", blockedKeys, 3);

        // Then
        assertNull(expectedEncodeOptions);
    }

    // Reflection helpers for invoking private static methods
    private static Object invokePrivateStatic(final String methodName, final Class<?>[] paramTypes,
            final Object... args) throws Exception {
        final Method declaredMethod = ObjectEncoder.class.getDeclaredMethod(methodName, paramTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, args);
    }
}
