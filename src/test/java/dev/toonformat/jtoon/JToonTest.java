package dev.toonformat.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.toonformat.jtoon.TestPojos.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test class for JToon encoder, converted from TypeScript vitest tests.
 */
@Tag("unit")
class JToonTest {

    // Helper to create a LinkedHashMap for objects, preserving insertion order
    private static Map<String, Object> obj(Object... kvs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            map.put((String) kvs[i], kvs[i + 1]);
        }
        return map;
    }

    // Helper for simple list creation
    @SafeVarargs
    private static <T> List<T> list(T... values) {
        return List.of(values);
    }

    // Helper for encode with default options
    private static String encode(Object input) {
        return JToon.encode(input);
    }

    // Helper for encode with custom options
    private static String encode(Object input, EncodeOptions options) {
        return JToon.encode(input, options);
    }

    @Nested
    @DisplayName("primitives")
    class Primitives {

        @Test
        @DisplayName("encodes safe strings without quotes")
        void encodesSafeStrings() {
            assertEquals("hello", encode("hello"));
            assertEquals("Ada_99", encode("Ada_99"));
        }

        @Test
        @DisplayName("quotes empty string")
        void quotesEmptyString() {
            assertEquals("\"\"", encode(""));
        }

        @Test
        @DisplayName("quotes strings that look like booleans or numbers")
        void quotesAmbiguousStrings() {
            assertEquals("\"true\"", encode("true"));
            assertEquals("\"false\"", encode("false"));
            assertEquals("\"null\"", encode("null"));
            assertEquals("\"42\"", encode("42"));
            assertEquals("\"-3.14\"", encode("-3.14"));
            assertEquals("\"1e-6\"", encode("1e-6"));
            assertEquals("\"05\"", encode("05"));
        }

        @Test
        @DisplayName("escapes control characters in strings")
        void escapesControlChars() {
            assertEquals("\"line1\\nline2\"", encode("line1\nline2"));
            assertEquals("\"tab\\there\"", encode("tab\there"));
            assertEquals("\"return\\rcarriage\"", encode("return\rcarriage"));
            assertEquals("\"C:\\\\Users\\\\path\"", encode("C:\\Users\\path"));
        }

        @Test
        @DisplayName("quotes strings with structural characters")
        void quotesStructuralChars() {
            assertEquals("\"[3]: x,y\"", encode("[3]: x,y"));
            assertEquals("\"- item\"", encode("- item"));
            assertEquals("\"[test]\"", encode("[test]"));
            assertEquals("\"{key}\"", encode("{key}"));
        }

        @Test
        @DisplayName("handles Unicode and emoji")
        void handlesUnicodeAndEmoji() {
            assertEquals("café", encode("café"));
            assertEquals("你好", encode("你好"));
            assertEquals("🚀", encode("🚀"));
            assertEquals("hello 👋 world", encode("hello 👋 world"));
        }

        @Test
        @DisplayName("encodes numbers")
        void encodesNumbers() {
            assertEquals("42", encode(42));
            assertEquals("3.14", encode(3.14));
            assertEquals("-7", encode(-7));
            assertEquals("0", encode(0));
        }

        @Test
        @DisplayName("handles special numeric values")
        void handlesSpecialNumericValues() {
            assertEquals("0", encode(-0.0));
            assertEquals("1000000", encode(1e6));
            assertEquals("0.000001", encode(1e-6));
            assertEquals("100000000000000000000", encode(1e20));
            assertEquals("0.000001", encode(0.000001));
            assertEquals("9007199254740991", encode(9007199254740991L));
        }

        @Test
        @DisplayName("encodes booleans")
        void encodesBooleans() {
            assertEquals("true", encode(true));
            assertEquals("false", encode(false));
        }

        @Test
        @DisplayName("encodes null")
        void encodesNull() {
            assertEquals("null", encode(null));
        }
    }

    @Nested
    @DisplayName("objects (simple)")
    class SimpleObjects {

        @Test
        @DisplayName("preserves key order in objects")
        void preservesKeyOrder() {
            Map<String, Object> obj = obj(
                "id", 123,
                "name", "Ada",
                "active", true);
            assertEquals("id: 123\nname: Ada\nactive: true", encode(obj));
        }

        @Test
        @DisplayName("encodes null values in objects")
        void encodesNullValues() {
            Map<String, Object> obj = obj("id", 123, "value", null);
            assertEquals("id: 123\nvalue: null", encode(obj));
        }

        @Test
        @DisplayName("encodes empty objects as empty string")
        void encodesEmptyObjects() {
            assertEquals("", encode(Map.of()));
        }

        @Test
        @DisplayName("quotes string values with special characters")
        void quotesSpecialChars() {
            assertEquals("note: \"a:b\"", encode(obj("note", "a:b")));
            assertEquals("note: \"a,b\"", encode(obj("note", "a,b")));
            assertEquals("text: \"line1\\nline2\"", encode(obj("text", "line1\nline2")));
            assertEquals("text: \"say \\\"hello\\\"\"", encode(obj("text", "say \"hello\"")));
        }

        @Test
        @DisplayName("quotes string values with leading/trailing spaces")
        void quotesWhitespace() {
            assertEquals("text: \" padded \"", encode(obj("text", " padded ")));
            assertEquals("text: \"  \"", encode(obj("text", "  ")));
        }

        @Test
        @DisplayName("quotes string values that look like booleans/numbers")
        void quotesAmbiguous() {
            assertEquals("v: \"true\"", encode(obj("v", "true")));
            assertEquals("v: \"42\"", encode(obj("v", "42")));
            assertEquals("v: \"-7.5\"", encode(obj("v", "-7.5")));
        }
    }

    @Nested
    @DisplayName("objects (keys)")
    class ObjectKeys {

        @Test
        @DisplayName("quotes keys with special characters")
        void quotesKeysWithSpecialChars() {
            assertEquals("\"order:id\": 7", encode(obj("order:id", 7)));
            assertEquals("\"[index]\": 5", encode(obj("[index]", 5)));
            assertEquals("\"{key}\": 5", encode(obj("{key}", 5)));
            assertEquals("\"a,b\": 1", encode(obj("a,b", 1)));
        }

        @Test
        @DisplayName("quotes keys with spaces or leading hyphens")
        void quotesKeysWithSpaces() {
            assertEquals("\"full name\": Ada", encode(obj("full name", "Ada")));
            assertEquals("\"-lead\": 1", encode(obj("-lead", 1)));
            assertEquals("\" a \": 1", encode(obj(" a ", 1)));
        }

        @Test
        @DisplayName("quotes numeric keys")
        void quotesNumericKeys() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("123", "x");
            assertEquals("\"123\": x", encode(map));
        }

        @Test
        @DisplayName("quotes empty string key")
        void quotesEmptyKey() {
            assertEquals("\"\": 1", encode(obj("", 1)));
        }

        @Test
        @DisplayName("escapes control characters in keys")
        void escapesControlCharsInKeys() {
            assertEquals("\"line\\nbreak\": 1", encode(obj("line\nbreak", 1)));
            assertEquals("\"tab\\there\": 2", encode(obj("tab\there", 2)));
        }

        @Test
        @DisplayName("escapes quotes in keys")
        void escapesQuotesInKeys() {
            assertEquals("\"he said \\\"hi\\\"\": 1", encode(obj("he said \"hi\"", 1)));
        }
    }

    @Nested
    @DisplayName("nested objects")
    class NestedObjects {

        @Test
        @DisplayName("encodes deeply nested objects")
        void encodesDeeplyNested() {
            Map<String, Object> obj = obj(
                "a", obj(
                    "b", obj(
                        "c", "deep")));
            assertEquals("a:\n  b:\n    c: deep", encode(obj));
        }

        @Test
        @DisplayName("encodes empty nested object")
        void encodesEmptyNested() {
            assertEquals("user:", encode(obj("user", Map.of())));
        }
    }

    @Nested
    @DisplayName("arrays of primitives")
    class PrimitiveArrays {

        @Test
        @DisplayName("encodes string arrays inline")
        void encodesStringArrays() {
            Map<String, Object> obj = obj("tags", list("reading", "gaming"));
            assertEquals("tags[2]: reading,gaming", encode(obj));
        }

        @Test
        @DisplayName("encodes number arrays inline")
        void encodesNumberArrays() {
            Map<String, Object> obj = obj("nums", list(1, 2, 3));
            assertEquals("nums[3]: 1,2,3", encode(obj));
        }

        @Test
        @DisplayName("encodes mixed primitive arrays inline")
        void encodesMixedPrimitiveArrays() {
            Map<String, Object> obj = obj("data", list("x", "y", true, 10));
            assertEquals("data[4]: x,y,true,10", encode(obj));
        }

        @Test
        @DisplayName("encodes empty arrays")
        void encodesEmptyArrays() {
            Map<String, Object> obj = obj("items", List.of());
            assertEquals("items[0]:", encode(obj));
        }

        @Test
        @DisplayName("handles empty string in arrays")
        void handlesEmptyStringInArrays() {
            Map<String, Object> obj = obj("items", list(""));
            assertEquals("items[1]: \"\"", encode(obj));
            Map<String, Object> obj2 = obj("items", list("a", "", "b"));
            assertEquals("items[3]: a,\"\",b", encode(obj2));
        }

        @Test
        @DisplayName("handles whitespace-only strings in arrays")
        void handlesWhitespaceOnlyStrings() {
            Map<String, Object> obj = obj("items", list(" ", "  "));
            assertEquals("items[2]: \" \",\"  \"", encode(obj));
        }

        @Test
        @DisplayName("quotes array strings with special characters")
        void quotesArrayStringsWithSpecialChars() {
            Map<String, Object> obj = obj("items", list("a", "b,c", "d:e"));
            assertEquals("items[3]: a,\"b,c\",\"d:e\"", encode(obj));
        }

        @Test
        @DisplayName("quotes strings that look like booleans/numbers in arrays")
        void quotesAmbiguousInArrays() {
            Map<String, Object> obj = obj("items", list("x", "true", "42", "-3.14"));
            assertEquals("items[4]: x,\"true\",\"42\",\"-3.14\"", encode(obj));
        }

        @Test
        @DisplayName("quotes strings with structural meanings in arrays")
        void quotesStructuralInArrays() {
            Map<String, Object> obj = obj("items", list("[5]", "- item", "{key}"));
            assertEquals("items[3]: \"[5]\",\"- item\",\"{key}\"", encode(obj));
        }
    }

    @Nested
    @DisplayName("arrays of objects (tabular and list items)")
    class ObjectArrays {

        @Test
        @DisplayName("encodes arrays of similar objects in tabular format")
        void encodesTabularFormat() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("sku", "A1", "qty", 2, "price", 9.99),
                    obj("sku", "B2", "qty", 1, "price", 14.5)));
            assertEquals("items[2]{sku,qty,price}:\n  A1,2,9.99\n  B2,1,14.5", encode(obj));
        }

        @Test
        @DisplayName("handles null values in tabular format")
        void handlesNullInTabular() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("id", 1, "value", null),
                    obj("id", 2, "value", "test")));
            assertEquals("items[2]{id,value}:\n  1,null\n  2,test", encode(obj));
        }

        @Test
        @DisplayName("quotes strings containing delimiters in tabular rows")
        void quotesDelimitersInTabular() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("sku", "A,1", "desc", "cool", "qty", 2),
                    obj("sku", "B2", "desc", "wip: test", "qty", 1)));
            assertEquals("items[2]{sku,desc,qty}:\n  \"A,1\",cool,2\n  B2,\"wip: test\",1", encode(obj));
        }

        @Test
        @DisplayName("quotes ambiguous strings in tabular rows")
        void quotesAmbiguousInTabular() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("id", 1, "status", "true"),
                    obj("id", 2, "status", "false")));
            assertEquals("items[2]{id,status}:\n  1,\"true\"\n  2,\"false\"", encode(obj));
        }

        @Test
        @DisplayName("handles tabular arrays with keys needing quotes")
        void handlesQuotedKeysInTabular() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("order:id", 1, "full name", "Ada"),
                    obj("order:id", 2, "full name", "Bob")));
            assertEquals("items[2]{\"order:id\",\"full name\"}:\n  1,Ada\n  2,Bob", encode(obj));
        }

        @Test
        @DisplayName("uses list format for objects with different fields")
        void usesListForDifferentFields() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("id", 1, "name", "First"),
                    obj("id", 2, "name", "Second", "extra", true)));
            assertEquals(
                """
                    items[2]:
                      - id: 1
                        name: First
                      - id: 2
                        name: Second
                        extra: true""",
                encode(obj));
        }

        @Test
        @DisplayName("uses list format for objects with nested values")
        void usesListForNestedValues() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("id", 1, "nested", obj("x", 1))));
            assertEquals(
                """
                    items[1]:
                      - id: 1
                        nested:
                          x: 1""",
                encode(obj));
        }

        @Test
        @DisplayName("preserves field order in list items")
        void preservesFieldOrderInListItems() {
            Map<String, Object> obj = obj("items", list(obj("nums", list(1, 2, 3), "name", "test")));
            assertEquals(
                """
                    items[1]:
                      - nums[3]: 1,2,3
                        name: test""",
                encode(obj));
        }

        @Test
        @DisplayName("preserves field order when primitive appears first")
        void preservesFieldOrderPrimitiveFirst() {
            Map<String, Object> obj = obj("items", list(obj("name", "test", "nums", list(1, 2, 3))));
            assertEquals(
                """
                    items[1]:
                      - name: test
                        nums[3]: 1,2,3""",
                encode(obj));
        }

        @Test
        @DisplayName("uses list format for objects containing arrays of arrays")
        void usesListForArrayOfArrays() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("matrix", list(list(1, 2), list(3, 4)), "name", "grid")));
            assertEquals(
                """
                    items[1]:
                      - matrix[2]:
                          - [2]: 1,2
                          - [2]: 3,4
                        name: grid""",
                encode(obj));
        }

        @Test
        @DisplayName("uses tabular format for nested uniform object arrays")
        void usesTabularForNestedUniformArrays() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("users", list(obj("id", 1, "name", "Ada"), obj("id", 2, "name", "Bob")), "status",
                        "active")));
            assertEquals(
                """
                    items[1]:
                      - users[2]{id,name}:
                          1,Ada
                          2,Bob
                        status: active""",
                encode(obj));
        }

        @Test
        @DisplayName("uses list format for nested object arrays with mismatched keys")
        void usesListForMismatchedKeys() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("users", list(obj("id", 1, "name", "Ada"), obj("id", 2)), "status", "active")));
            assertEquals(
                """
                    items[1]:
                      - users[2]:
                          - id: 1
                            name: Ada
                          - id: 2
                        status: active""",
                encode(obj));
        }

        @Test
        @DisplayName("uses list format for objects with multiple array fields")
        void usesListForMultipleArrays() {
            Map<String, Object> obj = obj("items",
                list(obj("nums", list(1, 2), "tags", list("a", "b"), "name", "test")));
            assertEquals(
                """
                    items[1]:
                      - nums[2]: 1,2
                        tags[2]: a,b
                        name: test""",
                encode(obj));
        }

        @Test
        @DisplayName("uses list format for objects with only array fields")
        void usesListForOnlyArrayFields() {
            Map<String, Object> obj = obj("items", list(obj("nums", list(1, 2, 3), "tags", list("a", "b"))));
            assertEquals(
                """
                    items[1]:
                      - nums[3]: 1,2,3
                        tags[2]: a,b""",
                encode(obj));
        }

        @Test
        @DisplayName("handles objects with empty arrays in list format")
        void handlesEmptyArraysInList() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("name", "test", "data", List.of())));
            assertEquals(
                """
                    items[1]:
                      - name: test
                        data[0]:""",
                encode(obj));
        }

        @Test
        @DisplayName("places first field of nested tabular arrays on hyphen line")
        void placesTabularOnHyphenLine() {
            Map<String, Object> obj = obj("items", list(obj("users", list(obj("id", 1), obj("id", 2)), "note", "x")));
            assertEquals(
                """
                    items[1]:
                      - users[2]{id}:
                          1
                          2
                        note: x""",
                encode(obj));
        }

        @Test
        @DisplayName("places empty arrays on hyphen line when first")
        void placesEmptyArrayOnHyphenLine() {
            Map<String, Object> obj = obj("items", list(obj("data", List.of(), "name", "x")));
            assertEquals(
                """
                    items[1]:
                      - data[0]:
                        name: x""",
                encode(obj));
        }

        @Test
        @DisplayName("uses field order from first object for tabular headers")
        void usesFirstObjectFieldOrder() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("a", 1, "b", 2, "c", 3),
                    obj("c", 30, "b", 20, "a", 10)));
            assertEquals("items[2]{a,b,c}:\n  1,2,3\n  10,20,30", encode(obj));
        }

        @Test
        @DisplayName("uses list format for one object with nested column")
        void usesListForNestedColumn() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("id", 1, "data", "string"),
                    obj("id", 2, "data", obj("nested", true))));
            assertEquals(
                """
                    items[2]:
                      - id: 1
                        data: string
                      - id: 2
                        data:
                          nested: true""",
                encode(obj));
        }
    }

    @Nested
    @DisplayName("arrays of arrays (primitives only)")
    class ArrayOfArrays {

        @Test
        @DisplayName("encodes nested arrays of primitives")
        void encodesNestedArrays() {
            Map<String, Object> obj = obj(
                "pairs", list(list("a", "b"), list("c", "d")));
            assertEquals("pairs[2]:\n  - [2]: a,b\n  - [2]: c,d", encode(obj));
        }

        @Test
        @DisplayName("quotes strings containing delimiters in nested arrays")
        void quotesDelimitersInNested() {
            Map<String, Object> obj = obj(
                "pairs", list(list("a", "b"), list("c,d", "e:f", "true")));
            assertEquals("pairs[2]:\n  - [2]: a,b\n  - [3]: \"c,d\",\"e:f\",\"true\"", encode(obj));
        }

        @Test
        @DisplayName("handles empty inner arrays")
        void handlesEmptyInnerArrays() {
            Map<String, Object> obj = obj(
                "pairs", list(List.of(), List.of()));
            assertEquals("pairs[2]:\n  - [0]:\n  - [0]:", encode(obj));
        }

        @Test
        @DisplayName("handles mixed-length inner arrays")
        void handlesMixedLengthArrays() {
            Map<String, Object> obj = obj(
                "pairs", list(list(1), list(2, 3)));
            assertEquals("pairs[2]:\n  - [1]: 1\n  - [2]: 2,3", encode(obj));
        }
    }

    @Nested
    @DisplayName("root arrays")
    class RootArrays {

        @Test
        @DisplayName("encodes arrays of primitives at root level")
        void encodesPrimitivesAtRoot() {
            List<Object> arr = list("x", "y", "true", true, 10);
            assertEquals("[5]: x,y,\"true\",true,10", encode(arr));
        }

        @Test
        @DisplayName("encodes arrays of similar objects in tabular format")
        void encodesTabularAtRoot() {
            List<Object> arr = list(obj("id", 1), obj("id", 2));
            assertEquals("[2]{id}:\n  1\n  2", encode(arr));
        }

        @Test
        @DisplayName("encodes arrays of different objects in list format")
        void encodesListAtRoot() {
            List<Object> arr = list(obj("id", 1), obj("id", 2, "name", "Ada"));
            assertEquals("[2]:\n  - id: 1\n  - id: 2\n    name: Ada", encode(arr));
        }

        @Test
        @DisplayName("encodes empty arrays at root level")
        void encodesEmptyAtRoot() {
            assertEquals("[0]:", encode(List.of()));
        }

        @Test
        @DisplayName("encodes arrays of arrays at root level")
        void encodesArrayOfArraysAtRoot() {
            List<Object> arr = list(list(1, 2), List.of());
            assertEquals("[2]:\n  - [2]: 1,2\n  - [0]:", encode(arr));
        }
    }

    @Nested
    @DisplayName("complex structures")
    class ComplexStructures {

        @Test
        @DisplayName("encodes objects with mixed arrays and nested objects")
        void encodesMixedStructures() {
            Map<String, Object> obj = obj(
                "user", obj(
                    "id", 123,
                    "name", "Ada",
                    "tags", list("reading", "gaming"),
                    "active", true,
                    "prefs", List.of()));
            assertEquals(
                """
                    user:
                      id: 123
                      name: Ada
                      tags[2]: reading,gaming
                      active: true
                      prefs[0]:""",
                encode(obj));
        }
    }

    @Nested
    @DisplayName("mixed arrays")
    class MixedArrays {

        @Test
        @DisplayName("uses list format for arrays mixing primitives and objects")
        void mixesPrimitivesAndObjects() {
            Map<String, Object> obj = obj(
                "items", list(1, obj("a", 1), "text"));
            assertEquals(
                """
                    items[3]:
                      - 1
                      - a: 1
                      - text""",
                encode(obj));
        }

        @Test
        @DisplayName("uses list format for arrays mixing objects and arrays")
        void mixesObjectsAndArrays() {
            Map<String, Object> obj = obj(
                "items", list(obj("a", 1), list(1, 2)));
            assertEquals(
                """
                    items[2]:
                      - a: 1
                      - [2]: 1,2""",
                encode(obj));
        }
    }

    @Nested
    @DisplayName("whitespace and formatting invariants")
    class Formatting {

        @Test
        @DisplayName("produces no trailing spaces at end of lines")
        void noTrailingSpaces() {
            Map<String, Object> obj = obj(
                "user", obj(
                    "id", 123,
                    "name", "Ada"),
                "items", list("a", "b"));
            String result = encode(obj);
            String[] lines = result.split("\n");
            for (String line : lines) {
                assertFalse(line.matches(".* $"), "Line has trailing space: '" + line + "'");
            }
        }

        @Test
        @DisplayName("produces no trailing newline at end of output")
        void noTrailingNewline() {
            Map<String, Object> obj = obj("id", 123);
            String result = encode(obj);
            assertFalse(result.matches(".*\\n$"), "Output has trailing newline");
        }
    }

    @Nested
    @DisplayName("non-JSON-serializable values")
    class NonJson {

        @Test
        @DisplayName("converts BigInt to string")
        void convertsBigInt() {
            assertEquals("123", encode(BigInteger.valueOf(123)));
            assertEquals("id: 456", encode(obj("id", BigInteger.valueOf(456))));
        }

        @Test
        @DisplayName("converts Date to ISO string")
        void convertsDate() {
            Instant date = Instant.parse("2025-01-01T00:00:00.000Z");
            assertEquals("\"2025-01-01T00:00:00Z\"", encode(date));
            assertEquals("created: \"2025-01-01T00:00:00Z\"", encode(obj("created", date)));
        }

        @Test
        @DisplayName("converts null to null")
        void convertsNull() {
            assertEquals("null", encode(null));
            assertEquals("value: null", encode(obj("value", null)));
        }

        @Test
        @DisplayName("converts non-finite numbers to null")
        void convertsNonFiniteNumbers() {
            String positive = encode(Double.POSITIVE_INFINITY);
            String negative = encode(Double.NEGATIVE_INFINITY);
            String nan = encode(Double.NaN);

            assertNotNull(positive);
            assertNotNull(negative);
            assertNotNull(nan);

            assertEquals("null", positive, "Positive Infinity should encode to null");
            assertEquals("null", negative, "Negative Infinity should encode to null");
            assertEquals("null", nan, "NaN should encode to null");
        }
    }

    @Nested
    @DisplayName("delimiter options")
    class DelimiterOptions {

        @Nested
        @DisplayName("basic delimiter usage")
        class BasicDelimiterUsage {

            @Test
            @DisplayName("encodes primitive arrays with tab")
            void encodesWithTab() {
                Map<String, Object> obj = obj("tags", list("reading", "gaming", "coding"));
                assertEquals("tags[3\t]: reading\tgaming\tcoding",
                    encode(obj, new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("encodes primitive arrays with pipe")
            void encodesWithPipe() {
                Map<String, Object> obj = obj("tags", list("reading", "gaming", "coding"));
                assertEquals("tags[3|]: reading|gaming|coding",
                    encode(obj, new EncodeOptions(2, Delimiter.PIPE, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("encodes primitive arrays with comma")
            void encodesWithComma() {
                Map<String, Object> obj = obj("tags", list("reading", "gaming", "coding"));
                assertEquals("tags[3]: reading,gaming,coding",
                    encode(obj, new EncodeOptions(2, Delimiter.COMMA, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("encodes tabular arrays with tab")
            void encodesTabularWithTab() {
                Map<String, Object> obj = obj(
                    "items", list(
                        obj("sku", "A1", "qty", 2, "price", 9.99),
                        obj("sku", "B2", "qty", 1, "price", 14.5)));
                assertEquals("items[2\t]{sku\tqty\tprice}:\n  A1\t2\t9.99\n  B2\t1\t14.5",
                    encode(obj, new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("encodes tabular arrays with pipe")
            void encodesTabularWithPipe() {
                Map<String, Object> obj = obj(
                    "items", list(
                        obj("sku", "A1", "qty", 2, "price", 9.99),
                        obj("sku", "B2", "qty", 1, "price", 14.5)));
                assertEquals("items[2|]{sku|qty|price}:\n  A1|2|9.99\n  B2|1|14.5",
                    encode(obj, new EncodeOptions(2, Delimiter.PIPE, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("encodes nested arrays with tab")
            void encodesNestedWithTab() {
                Map<String, Object> obj = obj("pairs", list(list("a", "b"), list("c", "d")));
                assertEquals("pairs[2\t]:\n  - [2\t]: a\tb\n  - [2\t]: c\td",
                    encode(obj, new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("encodes nested arrays with pipe")
            void encodesNestedWithPipe() {
                Map<String, Object> obj = obj("pairs", list(list("a", "b"), list("c", "d")));
                assertEquals("pairs[2|]:\n  - [2|]: a|b\n  - [2|]: c|d",
                    encode(obj, new EncodeOptions(2, Delimiter.PIPE, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("encodes root arrays with tab")
            void encodesRootWithTab() {
                List<Object> arr = list("x", "y", "z");
                assertEquals("[3\t]: x\ty\tz", encode(arr, new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("encodes root arrays with pipe")
            void encodesRootWithPipe() {
                List<Object> arr = list("x", "y", "z");
                assertEquals("[3|]: x|y|z", encode(arr, new EncodeOptions(2, Delimiter.PIPE, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("encodes root arrays of objects with tab")
            void encodesRootObjectsWithTab() {
                List<Object> arr = list(obj("id", 1), obj("id", 2));
                assertEquals("[2\t]{id}:\n  1\n  2", encode(arr, new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("encodes root arrays of objects with pipe")
            void encodesRootObjectsWithPipe() {
                List<Object> arr = list(obj("id", 1), obj("id", 2));
                assertEquals("[2|]{id}:\n  1\n  2", encode(arr, new EncodeOptions(2, Delimiter.PIPE, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }
        }

        @Nested
        @DisplayName("delimiter-aware quoting")
        class DelimiterQuoting {

            @Test
            @DisplayName("quotes strings containing tab")
            void quotesTab() {
                List<Object> input = list("a", "b\tc", "d");
                assertEquals("items[3\t]: a\t\"b\\tc\"\td",
                    encode(obj("items", input), new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("quotes strings containing pipe")
            void quotesPipe() {
                List<Object> input = list("a", "b|c", "d");
                assertEquals("items[3|]: a|\"b|c\"|d",
                    encode(obj("items", input), new EncodeOptions(2, Delimiter.PIPE, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("does not quote commas with tab")
            void doesNotQuoteCommasWithTab() {
                List<Object> input = list("a,b", "c,d");
                assertEquals("items[2\t]: a,b\tc,d",
                    encode(obj("items", input), new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("does not quote commas with pipe")
            void doesNotQuoteCommasWithPipe() {
                List<Object> input = list("a,b", "c,d");
                assertEquals("items[2|]: a,b|c,d",
                    encode(obj("items", input), new EncodeOptions(2, Delimiter.PIPE, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("quotes tabular values containing the delimiter")
            void quotesTabularDelimiter() {
                Map<String, Object> obj = obj(
                    "items", list(
                        obj("id", 1, "note", "a,b"),
                        obj("id", 2, "note", "c,d")));
                assertEquals("items[2]{id,note}:\n  1,\"a,b\"\n  2,\"c,d\"",
                    encode(obj, new EncodeOptions(2, Delimiter.COMMA, false, KeyFolding.OFF, Integer.MAX_VALUE)));
                assertEquals("items[2\t]{id\tnote}:\n  1\ta,b\n  2\tc,d",
                    encode(obj, new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("does not quote commas in object values with non-comma delimiter")
            void doesNotQuoteCommasInValues() {
                assertEquals("note: a,b", encode(obj("note", "a,b"), new EncodeOptions(2, Delimiter.PIPE, false, KeyFolding.OFF, Integer.MAX_VALUE)));
                assertEquals("note: a,b", encode(obj("note", "a,b"), new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }

            @Test
            @DisplayName("quotes nested array values containing the delimiter")
            void quotesNestedDelimiter() {
                assertEquals("pairs[1|]:\n  - [2|]: a|\"b|c\"",
                    encode(obj("pairs", list(list("a", "b|c"))), new EncodeOptions(2, Delimiter.PIPE, false, KeyFolding.OFF, Integer.MAX_VALUE)));
                assertEquals("pairs[1\t]:\n  - [2\t]: a\t\"b\\tc\"",
                    encode(obj("pairs", list(list("a", "b\tc"))), new EncodeOptions(2, Delimiter.TAB, false, KeyFolding.OFF, Integer.MAX_VALUE)));
            }
        }
    }

    @Nested
    @DisplayName("length marker option")
    class LengthMarker {

        @Test
        @DisplayName("adds length marker to primitive arrays")
        void addsMarkerToPrimitives() {
            Map<String, Object> obj = obj("tags", list("reading", "gaming", "coding"));
            assertEquals("tags[#3]: reading,gaming,coding", encode(obj, new EncodeOptions(2, Delimiter.COMMA, true, KeyFolding.OFF, Integer.MAX_VALUE)));
        }

        @Test
        @DisplayName("handles empty arrays")
        void handlesEmptyArrays() {
            assertEquals("items[#0]:", encode(obj("items", List.of()), new EncodeOptions(2, Delimiter.COMMA, true, KeyFolding.OFF, Integer.MAX_VALUE)));
        }

        @Test
        @DisplayName("adds length marker to tabular arrays")
        void addsMarkerToTabular() {
            Map<String, Object> obj = obj(
                "items", list(
                    obj("sku", "A1", "qty", 2, "price", 9.99),
                    obj("sku", "B2", "qty", 1, "price", 14.5)));
            assertEquals("items[#2]{sku,qty,price}:\n  A1,2,9.99\n  B2,1,14.5",
                encode(obj, new EncodeOptions(2, Delimiter.COMMA, true, KeyFolding.OFF, Integer.MAX_VALUE)));
        }

        @Test
        @DisplayName("adds length marker to nested arrays")
        void addsMarkerToNested() {
            Map<String, Object> obj = obj("pairs", list(list("a", "b"), list("c", "d")));
            assertEquals("pairs[#2]:\n  - [#2]: a,b\n  - [#2]: c,d",
                encode(obj, new EncodeOptions(2, Delimiter.COMMA, true, KeyFolding.OFF, Integer.MAX_VALUE)));
        }

        @Test
        @DisplayName("works with delimiter option")
        void worksWithDelimiter() {
            Map<String, Object> obj = obj("tags", list("reading", "gaming", "coding"));
            assertEquals("tags[#3|]: reading|gaming|coding", encode(obj, new EncodeOptions(2, Delimiter.PIPE, true, KeyFolding.OFF, Integer.MAX_VALUE)));
        }

        @Test
        @DisplayName("default is false (no length marker)")
        void defaultIsFalse() {
            Map<String, Object> obj = obj("tags", list("reading", "gaming", "coding"));
            assertEquals("tags[3]: reading,gaming,coding", encode(obj));
        }
    }

    @Nested
    @DisplayName("POJOs (Plain Old Java Objects)")
    class Pojos {

        @Nested
        @DisplayName("simple POJOs")
        class SimplePOJOs {

            @Test
            @DisplayName("encodes simple POJO with basic fields")
            void encodesSimplePOJO() {
                Person person = new Person("Ada", 30, true);
                assertEquals("name: Ada\nage: 30\nactive: true", encode(person));
            }

            @Test
            @DisplayName("encodes POJO with multiple field types")
            void encodesMultipleFieldTypes() {
                Product product = new Product(101, "Laptop", 999.99, true);
                assertEquals("id: 101\nname: Laptop\nprice: 999.99\ninStock: true", encode(product));
            }

            @Test
            @DisplayName("encodes POJO with null values")
            void encodesNullValues() {
                NullableData data = new NullableData("hello", null, null);
                assertEquals("text: hello\ncount: null\nflag: null", encode(data));
            }

            @Test
            @DisplayName("encodes POJO with all null values")
            void encodesAllNulls() {
                NullableData data = new NullableData(null, null, null);
                assertEquals("text: null\ncount: null\nflag: null", encode(data));
            }

            @Test
            @DisplayName("encodes POJO in object context")
            void encodesPOJOInObject() {
                Person person = new Person("Bob", 25, false);
                Map<String, Object> obj = obj("user", person);
                assertEquals("user:\n  name: Bob\n  age: 25\n  active: false", encode(obj));
            }
        }

        @Nested
        @DisplayName("nested POJOs and collections")
        class NestedAndCollections {

            @Test
            @DisplayName("encodes POJO with nested POJO")
            void encodesNestedPOJO() {
                Address address = new Address("123 Main St", "Springfield", "12345");
                Employee employee = new Employee("Alice", 1001, address);
                assertEquals(
                    """
                        name: Alice
                        id: 1001
                        address:
                          street: 123 Main St
                          city: Springfield
                          zipCode: "12345\"""",
                    encode(employee));
            }

            @Test
            @DisplayName("encodes deeply nested POJOs")
            void encodesDeeplyNested() {
                Address address = new Address("456 Oak Ave", "Metropolis", "54321");
                Employee manager = new Employee("Carol", 2001, address);
                Company company = new Company("TechCorp", manager);
                assertEquals(
                    """
                        name: TechCorp
                        manager:
                          name: Carol
                          id: 2001
                          address:
                            street: 456 Oak Ave
                            city: Metropolis
                            zipCode: "54321\"""",
                    encode(company));
            }

            @Test
            @DisplayName("encodes POJO with list of primitives")
            void encodesListOfPrimitives() {
                Skills skills = new Skills("Developer", List.of("Java", "Python", "JavaScript"));
                assertEquals("owner: Developer\nskillList[3]: Java,Python,JavaScript", encode(skills));
            }

            @Test
            @DisplayName("encodes POJO with list of POJOs in tabular format")
            void encodesListOfPOJOs() {
                Person person1 = new Person("Alice", 30, true);
                Person person2 = new Person("Bob", 25, false);
                Team team = new Team("DevTeam", List.of(person1, person2));
                assertEquals(
                    """
                        name: DevTeam
                        members[2]{name,age,active}:
                          Alice,30,true
                          Bob,25,false""",
                    encode(team));
            }

            @Test
            @DisplayName("encodes POJO with Map fields")
            void encodesMapFields() {
                Map<String, Object> settings = Map.of("debug", true, "timeout", 30, "mode", "production");
                Configuration config = new Configuration("AppConfig", settings);
                String result = encode(config);
                assertTrue(result.startsWith("name: AppConfig\nsettings:"));
                assertTrue(result.contains("debug: true"));
                assertTrue(result.contains("timeout: 30"));
                assertTrue(result.contains("mode: production"));
            }

            @Test
            @DisplayName("encodes POJO with empty collections")
            void encodesEmptyCollections() {
                EmptyCollections empty = new EmptyCollections(List.of(), Map.of());
                assertEquals("emptyList[0]:\nemptyMap:", encode(empty));
            }

            @Test
            @DisplayName("encodes POJO with multiple collection fields")
            void encodesMultipleCollections() {
                MultiCollection multi = new MultiCollection(
                    List.of(1, 2, 3),
                    List.of("a", "b"),
                    Map.of("x", 10, "y", 20));
                String result = encode(multi);
                assertTrue(result.contains("numbers[3]: 1,2,3"));
                assertTrue(result.contains("tags[2]: a,b"));
                assertTrue(result.contains("counts:"));
            }
        }

        @Nested
        @DisplayName("POJOs with Jackson annotations")
        class JacksonAnnotations {

            @Test
            @DisplayName("encodes POJO with @JsonProperty annotation")
            void encodesJsonProperty() {
                AnnotatedProduct product = new AnnotatedProduct(501, "Mouse", 29.99);
                assertEquals("product_id: 501\nproduct_name: Mouse\nprice: 29.99", encode(product));
            }

            @Test
            @DisplayName("encodes POJO with @JsonIgnore annotation")
            void encodesJsonIgnore() {
                SecureData data = new SecureData("public info", "secret", 1);
                assertEquals("publicField: public info\nversion: 1", encode(data));
            }

            @Test
            @DisplayName("encodes POJO with multiple annotations")
            void encodesMultipleAnnotations() {
                ComplexAnnotated obj = new ComplexAnnotated(123, "Test", "internal data", true);
                assertEquals("user_id: 123\nname: Test\nis_active: true", encode(obj));
            }

            @Test
            @DisplayName("encodes nested POJO with annotations")
            void encodesNestedWithAnnotations() {
                // Given
                Address address = new Address("789 Pine Rd", "Gotham", "99999");
                AnnotatedEmployee employee = new AnnotatedEmployee(3001, "Diana", address, "123-45-6789");

                // When
                String encode = encode(employee);

                // Then
                assertTrue(encode.contains("emp_id"));
                assertTrue(encode.contains("full_name"));
                assertFalse(encode.contains("ssn"));

                assertEquals(
                    """
                        emp_id: 3001
                        full_name: Diana
                        address:
                          street: 789 Pine Rd
                          city: Gotham
                          zipCode: "99999\"""",
                    encode);
            }

            @Test
            @DisplayName("encodes POJO with annotation: AnyGetter")
            void encodesWithAnyGetterAnnotations() {
                // Given
                Address address = new Address("789 Pine Rd", "Gotham", "99999");
                AnnotatedEmployee employee = new AnnotatedEmployee(3001, "Diana", address, "123-45-6789");

                TestPojos.FullEmployee fullEmployee = new TestPojos.FullEmployee(employee, Map.of("key1", "value1", "key2", "value2"));

                // When
                String encode = encode(fullEmployee);

                // Then
                assertTrue(encode.contains("key1: value1"));
                assertTrue(encode.contains("key2: value2"));
            }

            @Test
            @DisplayName("encodes POJO with annotation: PropertyOrder")
            void encodesWithJsonPropertyOrderAnnotations() {
                // Given
                Address address = new Address("789 Pine Rd", "Gotham", "99999");
                OrderEmployee orderEmployee = new OrderEmployee("Miles Edward O'Brien", 42, address);

                // When
                String encode = encode(orderEmployee);

                // Then
                assertEquals(
                    """
                        id: 42
                        name: Miles Edward O'Brien
                        address:
                          street: 789 Pine Rd
                          city: Gotham
                          zipCode: "99999\"""",
                    encode);
            }

            @Test
            @DisplayName("encodes POJO with annotation: JsonSerialize")
            void encodesWithJsonSerializeAnnotations() {
                // Given
                HotelInfoLlmRerankDTO hotelInfoLlmRerankDTO = new HotelInfoLlmRerankDTO("A 23",
                    "hotelId 23",
                    "hotelName",
                    "hotelBrand",
                    "hotelCategory",
                    "hotelPrice",
                    "hotelAddressDistance"
                );
                HotelInfoLlmRerankDTOWithSerializer hotel = new HotelInfoLlmRerankDTOWithSerializer("Sunset Hotel", hotelInfoLlmRerankDTO);

                // When
                String encode = encode(hotel);

                // Then
                assertEquals(
                    """
                        name: Sunset Hotel
                        hotelInfo: hotelId 23""",
                    encode);
            }

            @Test
            @DisplayName("encodes list of annotated POJOs in tabular format")
            void encodesListOfAnnotatedPOJOs() {
                AnnotatedProduct p1 = new AnnotatedProduct(101, "Keyboard", 79.99);
                AnnotatedProduct p2 = new AnnotatedProduct(102, "Monitor", 299.99);
                Map<String, Object> obj = obj("products", List.of(p1, p2));
                assertEquals(
                    """
                        products[2]{product_id,product_name,price}:
                          101,Keyboard,79.99
                          102,Monitor,299.99""",
                    encode(obj));
            }

            @Test
            @DisplayName("encodes nested POJO with keeping the order")
            void encodesNestedWithKeepingTheOrder() {
                List<HotelInfoLlmRerankDTO> hotelList = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    hotelList.add(new HotelInfoLlmRerankDTO("A" + (i + 1),
                        "hotelId " + i,
                        "hotelName",
                        "hotelBrand",
                        "hotelCategory",
                        "hotelPrice",
                        "hotelAddressDistance"
                    ));
                }

                assertTrue(encode(hotelList).startsWith("[5]{no,hotelId,hotelName,hotelBrand,hotelCategory,hotelPrice,hotelAddressDistance}:"));
            }
        }
    }

    @Test
    @DisplayName("throws unsupported Operation Exception for calling the constructor")
    void throwsOnConstructor() throws NoSuchMethodException {
        final Constructor<JToon> constructor = JToon.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final InvocationTargetException thrown =
            assertThrows(InvocationTargetException.class, constructor::newInstance);

        final Throwable cause = thrown.getCause();
        assertInstanceOf(UnsupportedOperationException.class, cause);
        assertEquals("Utility class cannot be instantiated", cause.getMessage());
    }
}
