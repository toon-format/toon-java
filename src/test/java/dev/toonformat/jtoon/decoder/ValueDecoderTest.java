package dev.toonformat.jtoon.decoder;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.PathExpansion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ValueDecoder utility class.
 * Tests decoding of primitive values, keys, and header formatting.
 */
@Tag("unit")
class ValueDecoderTest {

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        // Given
        final Constructor<ValueDecoder> constructor = ValueDecoder.class.getDeclaredConstructor();
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
    @DisplayName("parses list items whose first field is a tabular array")
    void decodeTabularArray() {
        // Given
        final String input = "items[1]:\n  - users[2]{id,name}:\n      1,Ada\n      2,Bob\n    status: active";

        // When
        final String result = ValueDecoder.decodeToJson(input, DecodeOptions.DEFAULT);

        // Then
        assertEquals("{\"items\":[{\"users\":[{\"id\":1,\"name\":\"Ada\"},"
                + "{\"id\":2,\"name\":\"Bob\"}],\"status\":\"active\"}]}", result);
    }

    @Test
    @DisplayName("parses arrays of arrays within objects")
    void decodeArraysOfArraysWithinObjects() {
        // Given
        final String input = "items[1]:\n  - matrix[2]:\n      - [2]: 1,2\n      - [2]: 3,4\n    name: grid";

        // When
        final String result = ValueDecoder.decodeToJson(input, DecodeOptions.DEFAULT);

        // Then
        assertEquals("{\"items\":[{\"matrix\":[[1,2],[3,4]],\"name\":\"grid\"}]}", result);
    }

    @Test
    void decode_returnsEmptyMap_whenProcessedIsEmpty() {
        // Given
        final String input = "   ";  // only whitespace

        // When
        final DecodeOptions options = new DecodeOptions();
        final Object result = ValueDecoder.decode(input, options);

        // Then
        assertInstanceOf(LinkedHashMap.class, result, "Result must be an empty LinkedHashMap");
        assertTrue(((LinkedHashMap<?, ?>) result).isEmpty(), "Map must be empty");
    }

    @Test
    @DisplayName("strips a single leading byte-order mark before decoding")
    void decode_stripsLeadingByteOrderMark() {
        // Given
        final String input = Character.toString(0xFEFF) + "name: Ada";

        // When
        final Object result = ValueDecoder.decode(input, DecodeOptions.DEFAULT);

        // Then
        assertEquals("{name=Ada}", result.toString());
    }

    @Test
    @DisplayName("keeps a byte-order mark that is not the first character as content")
    void decode_keepsNonLeadingByteOrderMarkAsContent() {
        // Given
        final String bom = Character.toString(0xFEFF);
        final String input = "name: " + bom + "Ada";

        // When
        final Object result = ValueDecoder.decode(input, DecodeOptions.DEFAULT);

        // Then
        assertEquals("{name=" + bom + "Ada}", result.toString());
    }

    @Test
    @DisplayName("discards full-line comment lines before structural parsing")
    void decode_discardsFullLineComments() {
        // Given
        final String input = "# a comment\nname: Ada";

        // When
        final Object result = ValueDecoder.decode(input, DecodeOptions.DEFAULT);

        // Then
        assertEquals("{name=Ada}", result.toString());
    }

    @Test
    @DisplayName("throws on a tab-indented hash line in strict mode, which is not a comment")
    void decode_throwsOnTabIndentedHashLineInStrictMode() {
        // Given
        // Spec §5.1: only U+0020 spaces may precede the '#', so a tab keeps the
        // line out of the comment pre-pass; §12 then rejects the tab as
        // indentation in strict mode.
        final String input = "items[1]{tag}:\n\t#a";

        // When
        assertThrows(IllegalArgumentException.class,
            () -> ValueDecoder.decode(input, DecodeOptions.DEFAULT));
    }

    @Test
    @DisplayName("decodes a tab-indented hash row as data in non-strict mode")
    void decode_tabIndentedHashRowInNonStrictMode() {
        // Given
        // Spec §12 non-strict: leading tabs are accepted as indentation and
        // removed from the line's content before classification (§5.2).
        final String input = "items[3]{id}:\n  1\n\t#x\n  2";

        // When
        final Object result = ValueDecoder.decode(input, DecodeOptions.withStrict(false));

        // Then
        assertEquals("{items=[{id=1}, {id=#x}, {id=2}]}", result.toString());
    }

    @Test
    @DisplayName("expands a leading tab to one indentation level in non-strict mode")
    void decode_expandsLeadingTabToOneIndentLevelInNonStrictMode() {
        // Given
        // Spec §12: depth computation for tabs is implementation-defined; JToon
        // expands each leading tab to indentSize spaces (one level).
        final String input = "outer:\n\tinner: 1";

        // When
        final Object result = ValueDecoder.decode(input, DecodeOptions.withStrict(false));

        // Then
        assertEquals("{outer={inner=1}}", result.toString());
    }

    @Test
    @DisplayName("treats a hash not at line start as data")
    void decode_hashInsideLineIsData() {
        // When
        final Object result = ValueDecoder.decode("name: a#b", DecodeOptions.DEFAULT);

        // Then
        assertEquals("{name=a#b}", result.toString());
    }

    @Test
    @DisplayName("decodes an empty array literal as an empty list")
    void decode_emptyArrayLiteral() {
        // When
        final Object result = ValueDecoder.decode("[]", DecodeOptions.DEFAULT);

        // Then
        assertEquals("[]", result.toString());
    }

    @Test
    @DisplayName("decodes a keyless root array header as an array")
    void decode_keylessRootArrayHeader() {
        // When
        final Object result = ValueDecoder.decode("[2]: 1,2", DecodeOptions.DEFAULT);

        // Then
        assertEquals("[1, 2]", result.toString());
    }

    @Test
    @DisplayName("decodes a keyed tabular root header as a keyed object")
    void decode_keyedTabularRootHeader() {
        // Given
        final String input = "servers[1:]{host,port}:\n  alpha: a.example.com,8080";

        // When
        final Object result = ValueDecoder.decode(input, DecodeOptions.DEFAULT);

        // Then
        assertEquals("{servers={alpha={host=a.example.com, port=8080}}}", result.toString());
    }

    @Test
    @DisplayName("parses a bare scalar at the root")
    void decode_bareScalarRoot() {
        // When
        final Object result = ValueDecoder.decode("42", DecodeOptions.DEFAULT);

        // Then
        assertEquals("42", result.toString());
    }

    @Test
    @DisplayName("rejects stray unquoted brackets in a root key in strict mode")
    void decode_strict_rejectsStrayBracketsInRootKey() {
        // When / Then
        assertThrows(IllegalArgumentException.class,
            () -> ValueDecoder.decode("foo[1][bar]: x", DecodeOptions.DEFAULT));
    }

    @Test
    @DisplayName("rejects unquoted brackets without a valid header in strict mode")
    void decode_strict_rejectsBracketsWithoutValidHeader() {
        // When / Then
        assertThrows(IllegalArgumentException.class,
            () -> ValueDecoder.decode("items[2]{id,name}", DecodeOptions.DEFAULT));
    }

    @Test
    @DisplayName("Should parse TOON format primitive array to JSON")
    void parsePrimitiveArray() {
        // When
        final Object parseValue = ValueDecoder.decode("items[3]: a,\"b,c\",\"d:e\"", DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{items=[a, b,c, d:e]}", parseValue.toString());
    }

    @Test
    @DisplayName("Should parse TOON format tabular array to JSON")
    void parseTabularArray() {
        // When
        final Object parseValue = ValueDecoder.decode(
                "items[2]{id,name}:\n  1,Alice\n  2,Bob\ncount: 2", DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{items=[{id=1, name=Alice}, {id=2, name=Bob}], count=2}", parseValue.toString());
    }

    @Test
    @DisplayName("Should parse TOON format nested array to JSON")
    void parseNestedArray() {
        // When
        final Object parseValue = ValueDecoder.decode(
            "items[1]:\n  - users[2]{id,name}:\n      1,Ada\n      2,Bob\n    status: active"
            , DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{items=[{users=[{id=1, name=Ada}, {id=2, name=Bob}], status=active}]}", parseValue.toString());
    }

    @Test
    @DisplayName("Should parse TOON format object to JSON")
    void parseObject() {
        // When
        final Object parseValue = ValueDecoder.decode("id: 123\nname: Ada\nactive: true", DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{id=123, name=Ada, active=true}", parseValue.toString());
    }

    @Test
    @DisplayName("Should parse TOON format number to JSON")
    void parseNumber() {
        // When
        final Object parseValue = ValueDecoder.decode("value: 1.5000", DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{value=1.5}", parseValue.toString());
    }

    @Test
    @DisplayName("Should parse TOON format to JSON tolerating whitespaces")
    void parseToleratingSpacesInCommas() {
        final Object parseValue = ValueDecoder.decode("tags[3]: a , b , c", DecodeOptions.DEFAULT);

        // Then
        assertNotNull(parseValue);
        assertEquals("{tags=[a, b, c]}", parseValue.toString());
    }

    @Test
    void givenNoLines_whenParse_thenReturnEmptyMap() {
        // Given
        final DecodeOptions decodeOptions = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        final Object parseValue = ValueDecoder.decode("  indented", decodeOptions);// depth=1

        // Then
        assertNotNull(parseValue);
        assertInstanceOf(Map.class, parseValue);
    }

    @Test
    void givenIndentedLineAndStrict_whenParse_thenThrow() {
        // Given
        final DecodeOptions decodeOptions = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> ValueDecoder.decode("  indented", decodeOptions));
    }

    @Test
    void decode_keyValuePair_callsKeyDecoder() {
        // Given
        final DecodeOptions decodeOptions = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

        // When
        final Object result = ValueDecoder.decode("name: Ada", decodeOptions);

        // Then
        // Whatever KeyDecoder returns, you simply assert expected behavior.
        // Usually: { "name" : "Ada" } as a map
        assertInstanceOf(Map.class, result);

        final Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(1, map.size());
        assertEquals("Ada", map.get("name"));
    }


    @Test
    void decodeToJson_throwsWrappedException_whenDecodeFails() {
        // Given
        final DecodeOptions options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);

        final String invalidIndentedInput = "  badIndent";

        // When
        final IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ValueDecoder.decodeToJson(invalidIndentedInput, options)
        );

        // Then
        assertTrue(ex.getMessage().contains("Failed to convert decoded value to JSON"));
        assertNotNull(ex.getCause());  // original decode() exception is preserved
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Unexpected indentation"));
    }

    @Test
    void givenInvalidInputAndStrictFalse_whenDecode_thenReturnsNull() {
        // Given — malformed quoted string causes StringEscaper to throw
        final DecodeOptions options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        final String invalidInput = "value: \"unclosed";

        // When
        final Object result = ValueDecoder.decode(invalidInput, options);

        // Then
        assertNull(result);
    }

    @Test
    void givenDecodeReturnsNull_whenDecodeToJson_thenReturnsNullLiteral() {
        // Given — malformed quoted string causes StringEscaper to throw
        final DecodeOptions options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        final String invalidInput = "value: \"unclosed";

        // When
        final String result = ValueDecoder.decodeToJson(invalidInput, options);

        // Then
        assertEquals("null", result);
    }

    @Test
    void givenNullLiteralInput_whenDecodeToJson_thenReturnsNullLiteral() {
        // Given
        final String input = "null";

        // When
        final String result = ValueDecoder.decodeToJson(input, DecodeOptions.DEFAULT);

        // Then
        assertEquals("null", result);
    }

    @Test
    void givenValidInputAndStrictFalse_whenDecode_thenReturnsResult() {
        // Given
        final DecodeOptions options = new DecodeOptions(2, Delimiter.COMMA, false, PathExpansion.OFF,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        final String validInput = "name: Ada";

        // When
        final Object result = ValueDecoder.decode(validInput, options);

        // Then
        assertNotNull(result);
        assertInstanceOf(Map.class, result);
        assertEquals("Ada", ((Map<?, ?>) result).get("name"));
    }

}
