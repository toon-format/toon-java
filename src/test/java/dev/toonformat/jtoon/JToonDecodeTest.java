package dev.toonformat.jtoon;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class JToonDecodeTest {

    private static final long DECODED_VALUE_42 = 42L;
    private static final long DECODED_ID_123 = 123L;
    private static final double DECODED_PI = 3.14;
    private static final double ASSERT_DELTA = 0.0001;
    private static final int EXPECTED_TAG_COUNT = 3;

    @Nested
    @DisplayName("Primitives")
    class Primitives {

        @Test
        @DisplayName("should decode null")
        void testNull() {
            // Given
            final Object result = JToon.decode("value: null");

            // Then
            assertInstanceOf(Map.class, result);
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertNull(map.get("value"));
        }

        @Test
        @DisplayName("should decode booleans")
        void testBooleans() {
            // Given
            final Object result1 = JToon.decode("active: true");

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map1 = (Map<String, Object>) result1;
            assertEquals(true, map1.get("active"));
        }

        @Test
        @DisplayName("should decode booleans")
        void testBooleans2() {
            // Given
            final Object result2 = JToon.decode("active: false");

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map2 = (Map<String, Object>) result2;
            assertEquals(false, map2.get("active"));
        }

        @Test
        @DisplayName("should decode integers")
        void testIntegers() {
            // Given
            final Object result = JToon.decode("count: 42");

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(DECODED_VALUE_42, map.get("count"));
        }

        @Test
        @DisplayName("should decode floating point numbers")
        void testFloatingPoint() {
            // Given
            final Object result = JToon.decode("price: 3.14");

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(DECODED_PI, (Double) map.get("price"), ASSERT_DELTA);
        }

        @Test
        @DisplayName("should decode unquoted strings")
        void testUnquotedStrings() {
            // Given
            final Object result = JToon.decode("name: Ada");

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("Ada", map.get("name"));
        }

        @Test
        @DisplayName("should decode quoted strings")
        void testQuotedStrings() {
            // Given
            final Object result = JToon.decode("note: \"hello, world\"");

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("hello, world", map.get("note"));
        }

        @Test
        @DisplayName("should decode strings with escape sequences")
        void testEscapedStrings() {
            // Given
            final Object result = JToon.decode("text: \"line1\\nline2\"");

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("line1\nline2", map.get("text"));
        }

        @Test
        @DisplayName("should decode unicode escape sequences")
        void testUnicodeEscapedStrings() {
            // Given
            final Object result = JToon.decode("val: \"a\\u0004b\"");

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("a\004b", map.get("val"));
        }
    }

    @Nested
    @DisplayName("Simple Objects")
    class SimpleObjects {

        @Test
        @DisplayName("should decode simple object")
        void testSimpleObject() {
            // Given
            final String toon = """
                id: 123
                name: Ada
                active: true
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(DECODED_ID_123, map.get("id"));
            assertEquals("Ada", map.get("name"));
            assertEquals(true, map.get("active"));
        }
    }

    @Nested
    @DisplayName("Nested Objects")
    class NestedObjects {

        @Test
        @DisplayName("should decode nested object")
        void testNestedObject() {
            // Given
            final String toon = """
                user:
                  id: 123
                  name: Ada
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final Map<String, Object> user = (Map<String, Object>) map.get("user");
            assertEquals(DECODED_ID_123, user.get("id"));
            assertEquals("Ada", user.get("name"));
        }

        @Test
        @DisplayName("should decode deeply nested object")
        void testDeeplyNestedObject() {
            // Given
            final String toon = """
                user:
                  id: 123
                  contact:
                    email: ada@example.com
                    phone: "555-1234"
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final Map<String, Object> user = (Map<String, Object>) map.get("user");
            @SuppressWarnings("unchecked")
            final Map<String, Object> contact = (Map<String, Object>) user.get("contact");
            assertEquals("ada@example.com", contact.get("email"));
            assertEquals("555-1234", contact.get("phone"));
        }
    }

    @Nested
    @DisplayName("Primitive Arrays")
    class PrimitiveArrays {

        @Test
        @DisplayName("should decode inline primitive array")
        void testInlinePrimitiveArray() {
            // Given
            final String toon = "tags[3]: reading,gaming,coding";

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(EXPECTED_TAG_COUNT, tags.size());
            assertEquals("reading", tags.get(0));
            assertEquals("gaming", tags.get(1));
            assertEquals("coding", tags.get(2));
        }

        @Test
        @DisplayName("should reject primitive values on the line below the header")
        void rejectsPrimitiveValuesBelowHeader() {
            // Given
            final String toon = """
                tags[3]:
                  reading,gaming,coding
                """;

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> JToon.decode(toon));
        }

        @Test
        @DisplayName("should decode array with mixed primitives")
        void testMixedPrimitiveArray() {
            // Given
            final String toon = "values[4]: 42,3.14,\"true\",null";

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> values = (List<Object>) map.get("values");
            assertEquals(DECODED_VALUE_42, values.get(0));
            assertEquals(DECODED_PI, (Double) values.get(1), ASSERT_DELTA);
            assertEquals("true", values.get(2));
            final int fourthIndex = 3;
            assertNull(values.get(fourthIndex));
        }

        @Test
        @DisplayName("should decode empty array")
        void testEmptyArray() {
            // Given
            final String toon = "items[0]:";

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> items = (List<Object>) map.get("items");
            assertEquals(0, items.size());
        }

        @Test
        @DisplayName("should decode canonical empty array field")
        void testCanonicalEmptyArrayField() {
            // Given
            final Object result = JToon.decode("items: []");

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> items = (List<Object>) map.get("items");
            assertTrue(items.isEmpty());
        }
    }

    @Nested
    @DisplayName("Tabular Arrays")
    class TabularArrays {

        @Test
        @DisplayName("should decode tabular array")
        void testTabularArray() {
            // Given
            final String toon = """
                users[2]{id,name,role}:
                  1,Alice,admin
                  2,Bob,user
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> users = (List<Object>) map.get("users");
            assertEquals(2, users.size());

            @SuppressWarnings("unchecked")
            final Map<String, Object> user1 = (Map<String, Object>) users.get(0);
            assertEquals(1L, user1.get("id"));
            assertEquals("Alice", user1.get("name"));
            assertEquals("admin", user1.get("role"));

            @SuppressWarnings("unchecked")
            final Map<String, Object> user2 = (Map<String, Object>) users.get(1);
            assertEquals(2L, user2.get("id"));
            assertEquals("Bob", user2.get("name"));
            assertEquals("user", user2.get("role"));
        }

        @Test
        @DisplayName("should decode tabular array with mixed types")
        void testTabularArrayMixedTypes() {
            // Given
            final String toon = """
                items[2]{sku,qty,price}:
                  A1,2,9.99
                  B2,1,14.5
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> items = (List<Object>) map.get("items");

            @SuppressWarnings("unchecked")
            final Map<String, Object> item1 = (Map<String, Object>) items.get(0);
            assertEquals("A1", item1.get("sku"));
            assertEquals(2L, item1.get("qty"));
            final double expectedPrice = 9.99;
            assertEquals(expectedPrice, (Double) item1.get("price"), ASSERT_DELTA);
        }

        @Test
        @DisplayName("should decode tabular array with quoted values")
        void testTabularArrayQuotedValues() {
            // Given
            final String toon = """
                items[2]{id,name}:
                  1,"First Item"
                  2,"Second, Item"
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> items = (List<Object>) map.get("items");

            @SuppressWarnings("unchecked")
            final Map<String, Object> item2 = (Map<String, Object>) items.get(1);
            assertEquals("Second, Item", item2.get("name"));
        }
    }

    @Nested
    @DisplayName("List Arrays")
    class ListArrays {

        @Test
        @DisplayName("should decode list array with simple items")
        void testSimpleListArray() {
            // Given
            final String toon = """
                items[2]:
                  - first
                  - second
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> items = (List<Object>) map.get("items");
            assertEquals("first", items.get(0));
            assertEquals("second", items.get(1));
        }

        @Test
        @DisplayName("should decode list array with object items")
        void testListArrayWithObjects() {
            // Given
            final String toon = """
                items[2]:
                  - id: 1
                    name: First
                  - id: 2
                    name: Second
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> items = (List<Object>) map.get("items");

            @SuppressWarnings("unchecked")
            final Map<String, Object> item1 = (Map<String, Object>) items.get(0);
            assertEquals(1L, item1.get("id"));
            assertEquals("First", item1.get("name"));

            @SuppressWarnings("unchecked")
            final Map<String, Object> item2 = (Map<String, Object>) items.get(1);
            assertEquals(2L, item2.get("id"));
            assertEquals("Second", item2.get("name"));
        }
    }

    @Nested
    @DisplayName("Delimiter Support")
    class DelimiterSupport {

        @Test
        @DisplayName("should decode comma-delimited array")
        void testCommaDelimiter() {
            // Given
            final String toon = "tags[3]: a,b,c";

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(EXPECTED_TAG_COUNT, tags.size());
        }

        @Test
        @DisplayName("should decode tab-delimited array")
        void testTabDelimiter() {
            // Given
            final String toon = "tags[3\t]: a\tb\tc";
            final DecodeOptions options = DecodeOptions.withDelimiter(Delimiter.TAB);


            // When
            final Object result = JToon.decode(toon, options);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(EXPECTED_TAG_COUNT, tags.size());
            assertEquals("a", tags.get(0));
            assertEquals("b", tags.get(1));
            assertEquals("c", tags.get(2));
        }

        @Test
        @DisplayName("should decode pipe-delimited array")
        void testPipeDelimiter() {
            // Given
            final String toon = "tags[3|]: a|b|c";
            final DecodeOptions options = DecodeOptions.withDelimiter(Delimiter.PIPE);

            // When
            final Object result = JToon.decode(toon, options);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(EXPECTED_TAG_COUNT, tags.size());
        }
    }

    @Nested
    @DisplayName("Complex Structures")
    class ComplexStructures {

        @Test
        @DisplayName("should decode object with nested arrays")
        void testObjectWithNestedArrays() {
            // Given
            final String toon = """
                user:
                  id: 123
                  name: Ada
                  tags[2]: dev,admin
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final Map<String, Object> user = (Map<String, Object>) map.get("user");
            @SuppressWarnings("unchecked")
            final List<Object> tags = (List<Object>) user.get("tags");
            assertEquals(2, tags.size());
        }

        @Test
        @DisplayName("should decode array of nested objects")
        void testArrayOfNestedObjects() {
            // Given
            final String toon = """
                users[2]{id,name}:
                  1,Alice
                  2,Bob
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            final List<Object> users = (List<Object>) map.get("users");
            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("should decode mixed content at root level")
        void testMixedRootContent() {
            // Given
            final String toon = """
                id: 123
                name: Ada
                tags[2]: dev,admin
                active: true
                """;

            // When
            final Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(DECODED_ID_123, map.get("id"));
            assertEquals("Ada", map.get("name"));
            assertEquals(true, map.get("active"));
            @SuppressWarnings("unchecked")
            final List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(2, tags.size());
        }

        @Test
        @DisplayName("should decode canonical empty root array")
        void testCanonicalEmptyRootArray() {
            // When
            final Object result = JToon.decode("[]");

            // Then
            assertInstanceOf(List.class, result);
            assertEquals(List.of(), result);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should handle empty input")
        void testEmptyInput() {
            // Then
            assertEquals(Collections.emptyMap(), JToon.decode(""));
            assertEquals(Collections.emptyMap(), JToon.decode("   "));
            assertEquals(Collections.emptyMap(), JToon.decode(null));
        }

        @Test
        @DisplayName("should throw in strict mode for invalid array header")
        void testStrictModeError() {
            // Given
            final String toon = "[invalid]: 1";

            // When
            final DecodeOptions options = DecodeOptions.withStrict(true);

            // Then
            assertThrows(IllegalArgumentException.class, () -> JToon.decode(toon, options));
        }

        @Test
        @DisplayName("should read an invalid array header as a key in lenient mode")
        void testLenientMode() {
            // Given
            final String toon = "[invalid]: 1";
            final DecodeOptions options = DecodeOptions.withStrict(false);

            // When
            final Object result = JToon.decode(toon, options);

            // Then
            assertEquals(Map.of("[invalid]", 1L), result);
        }

        @Test
        @DisplayName("strict mode: throws on duplicate sibling keys")
        void strictDuplicateSiblingKeys() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("name: Ada\nname: Bob"));
        }

        @Test
        @DisplayName("strict mode: throws on nested duplicate sibling keys")
        void strictNestedDuplicateKeys() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("""
                    outer:
                      name: Ada
                      name: Bob
                    """));
        }

        @Test
        @DisplayName("strict mode: throws on duplicate keys within a list-item object")
        void strictDuplicateKeysInListItem() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("""
                    items[1]:
                      - id: 1
                        id: 2
                    """));
        }

        @Test
        @DisplayName("strict mode: throws on extra brackets between bracket segment and colon")
        void strictExtraBrackets() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("foo[1][bar]: 10"));
        }

        @Test
        @DisplayName("strict mode: throws on non-integer bracket segment")
        void strictNonIntegerBracket() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("foo[bar]: 10"));
        }

        @Test
        @DisplayName("strict mode: throws on text between bracket segment and colon")
        void strictTextBetweenBracketAndColon() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("foo[2]extra: a,b"));
        }

        @Test
        @DisplayName("strict mode: throws on negative bracket length")
        void strictNegativeBracketLength() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("items[-1]: a,b,c"));
        }

        @Test
        @DisplayName("strict mode: throws on bracket length with leading zeros")
        void strictLeadingZeroBracketLength() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("items[03]: a,b,c"));
        }

        @Test
        @DisplayName("strict mode: throws on array header missing colon")
        void strictMissingColonInArrayHeader() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("""
                    items[2]{id,name}
                      1,Ada
                      2,Bob
                    """));
        }

        @Test
        @DisplayName("strict mode: throws on whitespace between bracket segment and colon")
        void strictWhitespaceBetweenBracketAndColon() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("items[2] :\n  1,2"));
        }

        @Test
        @DisplayName("strict mode: throws on whitespace between bracket and fields segment")
        void strictWhitespaceBetweenBracketAndFields() {
            assertThrows(IllegalArgumentException.class,
                () -> JToon.decode("items[2] {a,b}:\n  1,2\n  3,4"));
        }

        @Test
        @DisplayName("strict mode: reads a field list spanning the colon as a key-value line")
        void strictReadsFieldListSpanningColonAsKeyValue() {
            assertEquals(Map.of("[1]{x", "y}"), JToon.decode("[1]{x:y}"));
            assertEquals(Map.of("a[1]{x", "y}"), JToon.decode("a[1]{x:y}"));
        }

        @Test
        @DisplayName("strict mode: throws on a header it would otherwise drop content after")
        void strictRejectsContentAfterHeader() {
            assertThrows(IllegalArgumentException.class, () -> JToon.decode("items[0]{a,b}: 1,2"));
            assertThrows(IllegalArgumentException.class, () -> JToon.decode("[1]{a,b}}:\n  1,2"));
            assertThrows(IllegalArgumentException.class, () -> JToon.decode("a[1]:\n  - [1]{a}: 1"));
        }

        @Test
        @DisplayName("lenient mode: reads a fields-bearing header with inline content as a key")
        void lenientReadsFieldsHeaderWithInlineContentAsKey() {
            final DecodeOptions lenient = DecodeOptions.withStrict(false);
            assertEquals(Map.of("items[0]{a,b}", "1,2"), JToon.decode("items[0]{a,b}: 1,2", lenient));
            assertEquals(Map.of("a", List.of(Map.of("[1]{a}", 1L))), JToon.decode("a[1]:\n  - [1]{a}: 1", lenient));
        }

        @Test
        @DisplayName("throws on an indented root null or empty array in either mode")
        void rejectsIndentedRootLiteral() {
            final DecodeOptions lenient = DecodeOptions.withStrict(false);
            assertThrows(IllegalArgumentException.class, () -> JToon.decode("  null"));
            assertThrows(IllegalArgumentException.class, () -> JToon.decode("  []"));
            assertThrows(IllegalArgumentException.class, () -> JToon.decode("  null", lenient));
            assertThrows(IllegalArgumentException.class, () -> JToon.decode("  []", lenient));
        }

        @Test
        @DisplayName("lenient mode: allows brackets in keys")
        void lenientAllowsBracketsInKeys() {
            final DecodeOptions lenient = DecodeOptions.withStrict(false);
            final Object result = JToon.decode("foo[1][bar]: 10", lenient);
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            final long decodedTen = 10L;
            assertEquals(decodedTen, map.get("foo[1][bar]"));
        }

        @Test
        @DisplayName("lenient mode: allows duplicate keys (last-write-wins)")
        void lenientAllowsDuplicateKeys() {
            final DecodeOptions lenient = DecodeOptions.withStrict(false);
            final Object result = JToon.decode("name: Ada\nname: Bob", lenient);
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("Bob", map.get("name"));
        }

        @Test
        @DisplayName("lenient mode: reads a leading-zero bracket length as part of the key")
        void lenientReadsLeadingZeroLengthAsKey() {
            final DecodeOptions lenient = DecodeOptions.withStrict(false);
            assertEquals(Map.of("items[03]", "a,b,c"), JToon.decode("items[03]: a,b,c", lenient));
        }
    }

    @Nested
    @DisplayName("DecodeToJson")
    class DecodeToJson {

        @Test
        @DisplayName("should decode to JSON string")
        void testDecodeToJson() {
            // Given
            final String toon = """
                id: 123
                name: Ada
                """;

            // When
            final String json = JToon.decodeToJson(toon);

            // Then
            assertNotNull(json);
            assertTrue(json.contains("123"));
            assertTrue(json.contains("Ada"));
        }

        @Test
        @DisplayName("should decode complex structure to JSON")
        void testComplexDecodeToJson() {
            // Given
            final String toon = """
                users[2]{id,name}:
                  1,Alice
                  2,Bob
                """;

            // When
            final String json = JToon.decodeToJson(toon);

            // Then
            assertNotNull(json);
            assertTrue(json.contains("users"));
            assertTrue(json.contains("Alice"));
            assertTrue(json.contains("Bob"));
        }

        @Test
        @DisplayName("should decode very complex structure to JSON, with empty Lists")
        void testVeryComplexDecodeToJson() {
            // Given
            final String toon = """
                [2]:
                  - name: Person.java
                    absolutePath: /Users/samples/petclinic/model/Person.java
                    types[1]:
                      - name: Person
                        lineNumber: 29
                        fields[2]{name,lineNumber}:
                          firstName,33
                          lastName,37
                        members[2]:
                          - name: getFirstName
                            readFields[1]{name,lineNumber}:
                              firstName,40
                            calledMethods[0]:
                            writtenFields[0]:
                            lineNumber: 39
                            signature: getFirstName()
                          - name: setFirstName
                            readFields[0]:
                            calledMethods[0]:
                            writtenFields[1]{name,lineNumber}:
                              firstName,44
                            lineNumber: 43
                            signature: setFirstName(java.lang.String)
                  - name: NamedEntity.java
                    absolutePath: /Users/samples/petclinic/model/NamedEntity.java
                    types[1]:
                      - name: NamedEntity
                        lineNumber: 32
                        fields[1]{name,lineNumber}:
                          name,36
                        members[1]:
                          - name: toString
                            readFields[3]{name,lineNumber}:
                              address,154
                              telephone,156
                              city,155
                            calledMethods[1]{name,lineNumber,signature}:
                              getFirstName,153,getFirstName
                            writtenFields[0]:
                            lineNumber: 47
                            signature: toString()
                """;

            // When
            final String json = JToon.decodeToJson(toon);

            // Then
            assertNotNull(json);
            assertTrue(json.contains("petclinic"));
        }
    }
}
