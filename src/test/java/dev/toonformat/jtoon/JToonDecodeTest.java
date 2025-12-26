package dev.toonformat.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class JToonDecodeTest {

    @Nested
    @DisplayName("Primitives")
    class Primitives {

        @Test
        @DisplayName("should decode null")
        void testNull() {
            // Given
            Object result = JToon.decode("value: null");

            // Then
            assertInstanceOf(Map.class, result);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertNull(map.get("value"));
        }

        @Test
        @DisplayName("should decode booleans")
        void testBooleans() {
            // Given
            Object result1 = JToon.decode("active: true");

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map1 = (Map<String, Object>) result1;
            assertEquals(true, map1.get("active"));
        }

        @Test
        @DisplayName("should decode booleans")
        void testBooleans2() {
            // Given
            Object result2 = JToon.decode("active: false");

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map2 = (Map<String, Object>) result2;
            assertEquals(false, map2.get("active"));
        }

        @Test
        @DisplayName("should decode integers")
        void testIntegers() {
            // Given
            Object result = JToon.decode("count: 42");

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(42L, map.get("count"));
        }

        @Test
        @DisplayName("should decode floating point numbers")
        void testFloatingPoint() {
            // Given
            Object result = JToon.decode("price: 3.14");

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(3.14, (Double) map.get("price"), 0.0001);
        }

        @Test
        @DisplayName("should decode unquoted strings")
        void testUnquotedStrings() {
            // Given
            Object result = JToon.decode("name: Ada");

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("Ada", map.get("name"));
        }

        @Test
        @DisplayName("should decode quoted strings")
        void testQuotedStrings() {
            // Given
            Object result = JToon.decode("note: \"hello, world\"");

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("hello, world", map.get("note"));
        }

        @Test
        @DisplayName("should decode strings with escape sequences")
        void testEscapedStrings() {
            // Given
            Object result = JToon.decode("text: \"line1\\nline2\"");

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("line1\nline2", map.get("text"));
        }
    }

    @Nested
    @DisplayName("Simple Objects")
    class SimpleObjects {

        @Test
        @DisplayName("should decode simple object")
        void testSimpleObject() {
            // Given
            String toon = """
                id: 123
                name: Ada
                active: true
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(123L, map.get("id"));
            assertEquals("Ada", map.get("name"));
            assertEquals(true, map.get("active"));
        }

        @Test
        @DisplayName("should decode object with quoted keys")
        void testQuotedKeys() {
            // Given
            String toon = "\"full name\": Alice";

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals("Alice", map.get("full name"));
        }

        @Test
        @DisplayName("should decode object with special character keys")
        void testSpecialCharacterKeys() {
            // Given
            String toon = "\"order:id\": 42";

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(42L, map.get("order:id"));
        }
    }

    @Nested
    @DisplayName("Nested Objects")
    class NestedObjects {

        @Test
        @DisplayName("should decode nested object")
        void testNestedObject() {
            // Given
            String toon = """
                user:
                  id: 123
                  name: Ada
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) map.get("user");
            assertEquals(123L, user.get("id"));
            assertEquals("Ada", user.get("name"));
        }

        @Test
        @DisplayName("should decode deeply nested object")
        void testDeeplyNestedObject() {
            // Given
            String toon = """
                user:
                  id: 123
                  contact:
                    email: ada@example.com
                    phone: "555-1234"
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) map.get("user");
            @SuppressWarnings("unchecked")
            Map<String, Object> contact = (Map<String, Object>) user.get("contact");
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
            String toon = "tags[3]: reading,gaming,coding";

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(3, tags.size());
            assertEquals("reading", tags.get(0));
            assertEquals("gaming", tags.get(1));
            assertEquals("coding", tags.get(2));
        }

        @Test
        @DisplayName("should decode multiline primitive array")
        void testMultilinePrimitiveArray() {
            // Given
            String toon = """
                tags[3]:
                  reading,gaming,coding
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(3, tags.size());
            assertEquals("reading", tags.get(0));
            assertEquals("gaming", tags.get(1));
            assertEquals("coding", tags.get(2));
        }

        @Test
        @DisplayName("should decode array with mixed primitives")
        void testMixedPrimitiveArray() {
            // Given
            String toon = "values[4]: 42,3.14,\"true\",null";

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) map.get("values");
            assertEquals(42L, values.get(0));
            assertEquals(3.14, (Double) values.get(1), 0.0001);
            assertEquals("true", values.get(2));
            assertNull(values.get(3));
        }

        @Test
        @DisplayName("should decode empty array")
        void testEmptyArray() {
            // Given
            String toon = "items[0]:";

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) map.get("items");
            assertEquals(0, items.size());
        }
    }

    @Nested
    @DisplayName("Tabular Arrays")
    class TabularArrays {

        @Test
        @DisplayName("should decode tabular array")
        void testTabularArray() {
            // Given
            String toon = """
                users[2]{id,name,role}:
                  1,Alice,admin
                  2,Bob,user
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> users = (List<Object>) map.get("users");
            assertEquals(2, users.size());

            @SuppressWarnings("unchecked")
            Map<String, Object> user1 = (Map<String, Object>) users.get(0);
            assertEquals(1L, user1.get("id"));
            assertEquals("Alice", user1.get("name"));
            assertEquals("admin", user1.get("role"));

            @SuppressWarnings("unchecked")
            Map<String, Object> user2 = (Map<String, Object>) users.get(1);
            assertEquals(2L, user2.get("id"));
            assertEquals("Bob", user2.get("name"));
            assertEquals("user", user2.get("role"));
        }

        @Test
        @DisplayName("should decode tabular array with mixed types")
        void testTabularArrayMixedTypes() {
            // Given
            String toon = """
                items[2]{sku,qty,price}:
                  A1,2,9.99
                  B2,1,14.5
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) map.get("items");

            @SuppressWarnings("unchecked")
            Map<String, Object> item1 = (Map<String, Object>) items.get(0);
            assertEquals("A1", item1.get("sku"));
            assertEquals(2L, item1.get("qty"));
            assertEquals(9.99, (Double) item1.get("price"), 0.0001);
        }

        @Test
        @DisplayName("should decode tabular array with quoted values")
        void testTabularArrayQuotedValues() {
            // Given
            String toon = """
                items[2]{id,name}:
                  1,"First Item"
                  2,"Second, Item"
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) map.get("items");

            @SuppressWarnings("unchecked")
            Map<String, Object> item2 = (Map<String, Object>) items.get(1);
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
            String toon = """
                items[2]:
                  - first
                  - second
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) map.get("items");
            assertEquals("first", items.get(0));
            assertEquals("second", items.get(1));
        }

        @Test
        @DisplayName("should decode list array with object items")
        void testListArrayWithObjects() {
            // Given
            String toon = """
                items[2]:
                  - id: 1
                    name: First
                  - id: 2
                    name: Second
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) map.get("items");

            @SuppressWarnings("unchecked")
            Map<String, Object> item1 = (Map<String, Object>) items.get(0);
            assertEquals(1L, item1.get("id"));
            assertEquals("First", item1.get("name"));

            @SuppressWarnings("unchecked")
            Map<String, Object> item2 = (Map<String, Object>) items.get(1);
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
            String toon = "tags[3]: a,b,c";

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(3, tags.size());
        }

        @Test
        @DisplayName("should decode tab-delimited array")
        void testTabDelimiter() {
            // Given
            String toon = "tags[3\t]:\ta\tb\tc";
            DecodeOptions options = DecodeOptions.withDelimiter(Delimiter.TAB);


            // When
            Object result = JToon.decode(toon, options);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(3, tags.size());
            assertEquals("a", tags.get(0));
            assertEquals("b", tags.get(1));
            assertEquals("c", tags.get(2));
        }

        @Test
        @DisplayName("should decode pipe-delimited array")
        void testPipeDelimiter() {
            // Given
            String toon = "tags[3|]: a|b|c";
            DecodeOptions options = DecodeOptions.withDelimiter(Delimiter.PIPE);

            // When
            Object result = JToon.decode(toon, options);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(3, tags.size());
        }
    }

    @Nested
    @DisplayName("Complex Structures")
    class ComplexStructures {

        @Test
        @DisplayName("should decode object with nested arrays")
        void testObjectWithNestedArrays() {
            // Given
            String toon = """
                user:
                  id: 123
                  name: Ada
                  tags[2]: dev,admin
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) map.get("user");
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) user.get("tags");
            assertEquals(2, tags.size());
        }

        @Test
        @DisplayName("should decode array of nested objects")
        void testArrayOfNestedObjects() {
            // Given
            String toon = """
                users[2]{id,name}:
                  1,Alice
                  2,Bob
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            @SuppressWarnings("unchecked")
            List<Object> users = (List<Object>) map.get("users");
            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("should decode mixed content at root level")
        void testMixedRootContent() {
            // Given
            String toon = """
                id: 123
                name: Ada
                tags[2]: dev,admin
                active: true
                """;

            // When
            Object result = JToon.decode(toon);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertEquals(123L, map.get("id"));
            assertEquals("Ada", map.get("name"));
            assertEquals(true, map.get("active"));
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) map.get("tags");
            assertEquals(2, tags.size());
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
            String toon = "[invalid]";  // Invalid array header format

            // When
            DecodeOptions options = DecodeOptions.withStrict(true);

            // Then
            assertThrows(IllegalArgumentException.class, () -> JToon.decode(toon, options));
        }

        @Test
        @DisplayName("should return null in lenient mode for invalid array header")
        void testLenientMode() {
            // Given
            String toon = "[invalid]";  // Invalid array header format
            DecodeOptions options = DecodeOptions.withStrict(false);

            // When
            Object result = JToon.decode(toon, options);

            // Then
            assertEquals(Collections.emptyList(), result);
        }
    }

    @Nested
    @DisplayName("DecodeToJson")
    class DecodeToJson {

        @Test
        @DisplayName("should decode to JSON string")
        void testDecodeToJson() {
            // Given
            String toon = """
                id: 123
                name: Ada
                """;

            // When
            String json = JToon.decodeToJson(toon);

            // Then
            assertNotNull(json);
            assertTrue(json.contains("123"));
            assertTrue(json.contains("Ada"));
        }

        @Test
        @DisplayName("should decode complex structure to JSON")
        void testComplexDecodeToJson() {
            // Given
            String toon = """
                users[2]{id,name}:
                  1,Alice
                  2,Bob
                """;

            // When
            String json = JToon.decodeToJson(toon);

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
            String toon = """
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
            String json = JToon.decodeToJson(toon);

            // Then
            assertNotNull(json);
            assertTrue(json.contains("petclinic"));
        }
    }
}
