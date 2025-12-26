package dev.toonformat.jtoon.encoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for LineWriter utility class.
 * Tests line accumulation and indentation logic for TOON format output.
 */
@Tag("unit")
class LineWriterTest {

    @Nested
    @DisplayName("Basic Line Writing")
    class BasicLineWriting {

        @Test
        @DisplayName("should write single line at depth 0")
        void testSingleLine() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "hello");

            //then
            assertEquals("hello", writer.toString());
        }

        @Test
        @DisplayName("should write multiple lines at depth 0")
        void testMultipleLines() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "line1");
            writer.push(0, "line2");
            writer.push(0, "line3");

            //then
            assertEquals("line1\nline2\nline3", writer.toString());
        }

        @Test
        @DisplayName("should return empty string for no lines")
        void testNoLines() {
            //given
            LineWriter writer = new LineWriter(2);

            //then
            assertEquals("", writer.toString());
        }
    }

    @Nested
    @DisplayName("Indentation with 2 Spaces")
    class IndentationTwoSpaces {

        @ParameterizedTest
        @DisplayName("should indent correctly based on depth")
        @CsvSource({
            "1, '  content'",
            "2, '    content'",
            "3, '      content'"
        })
        void testIndentationByDepth(int depth, String expected) {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(depth, "content");

            //then
            assertEquals(expected, writer.toString());
        }

        @Test
        @DisplayName("should handle mixed depths")
        void testMixedDepths() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "root");
            writer.push(1, "level1");
            writer.push(2, "level2");
            writer.push(1, "level1again");

            //then
            assertEquals("root\n  level1\n    level2\n  level1again", writer.toString());
        }
    }

    @Nested
    @DisplayName("Indentation with 4 Spaces")
    class IndentationFourSpaces {

        @Test
        @DisplayName("should indent depth 1 with 4 spaces")
        void testDepth1() {
            //given
            LineWriter writer = new LineWriter(4);

            //when
            writer.push(1, "content");

            //then
            assertEquals("    content", writer.toString());
        }

        @Test
        @DisplayName("should indent depth 2 with 8 spaces")
        void testDepth2() {
            //given
            LineWriter writer = new LineWriter(4);

            //when
            writer.push(2, "content");

            //then
            assertEquals("        content", writer.toString());
        }

        @Test
        @DisplayName("should handle nested structure")
        void testNestedStructure() {
            //given
            LineWriter writer = new LineWriter(4);

            //when
            writer.push(0, "user:");
            writer.push(1, "id: 1");
            writer.push(1, "address:");
            writer.push(2, "city: NYC");

            //then
            assertEquals("user:\n    id: 1\n    address:\n        city: NYC", writer.toString());
        }
    }

    @Nested
    @DisplayName("Content Types")
    class ContentTypes {

        @Test
        @DisplayName("should handle empty content")
        void testEmptyContent() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "");

            //then
            assertEquals("", writer.toString());
        }

        @ParameterizedTest
        @DisplayName("should handle various content types")
        @CsvSource({
            "hello world",
            "key: \"value\"",
            "Hello 世界",
            "Hello 🌍"
        })
        void testVariousContentTypes(String content) {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, content);

            //then
            assertEquals(content, writer.toString());
        }
    }

    @Nested
    @DisplayName("Real-World TOON Structures")
    class RealWorldStructures {

        @Test
        @DisplayName("should build simple object")
        void testSimpleObject() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "id: 123");
            writer.push(0, "name: Ada");
            writer.push(0, "active: true");

            //then
            assertEquals("id: 123\nname: Ada\nactive: true", writer.toString());
        }

        @Test
        @DisplayName("should build nested object")
        void testNestedObject() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "user:");
            writer.push(1, "id: 123");
            writer.push(1, "name: Ada");

            //then
            assertEquals("user:\n  id: 123\n  name: Ada", writer.toString());
        }

        @Test
        @DisplayName("should build array header with values")
        void testArrayWithValues() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "items[3]{id,name}:");
            writer.push(1, "1,Alice");
            writer.push(1, "2,Bob");
            writer.push(1, "3,Charlie");

            //then
            assertEquals("items[3]{id,name}:\n  1,Alice\n  2,Bob\n  3,Charlie", writer.toString());
        }

        @Test
        @DisplayName("should build list items")
        void testListItems() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "items[3]:");
            writer.push(1, "- id: 1");
            writer.push(2, "name: First");
            writer.push(1, "- id: 2");
            writer.push(2, "name: Second");

            //then
            assertEquals("items[3]:\n  - id: 1\n    name: First\n  - id: 2\n    name: Second", writer.toString());
        }

        @Test
        @DisplayName("should build deeply nested structure")
        void testDeeplyNested() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "root:");
            writer.push(1, "level1:");
            writer.push(2, "level2:");
            writer.push(3, "level3:");
            writer.push(4, "value: deep");

            //then
            assertEquals("root:\n  level1:\n    level2:\n      level3:\n        value: deep", writer.toString());
        }

        @Test
        @DisplayName("should build complex mixed structure")
        void testComplexMixedStructure() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "data:");
            writer.push(1, "users[2]{id,name}:");
            writer.push(2, "1,Alice");
            writer.push(2, "2,Bob");
            writer.push(1, "tags[3]: admin,user,guest");
            writer.push(1, "config:");
            writer.push(2, "enabled: true");

            //then
            String expected = """
                data:
                  users[2]{id,name}:
                    1,Alice
                    2,Bob
                  tags[3]: admin,user,guest
                  config:
                    enabled: true""";
            assertEquals(expected, writer.toString());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle depth 0 correctly")
        void testDepthZero() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(0, "content");

            //then
            assertEquals("content", writer.toString());
        }

        @Test
        @DisplayName("should handle very deep nesting")
        void testVeryDeepNesting() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            writer.push(10, "deep");

            //then
            assertEquals("                    deep", writer.toString());
        }

        @Test
        @DisplayName("should handle indentation size 1")
        void testIndentSize1() {
            //given
            LineWriter writer = new LineWriter(1);

            //when
            writer.push(0, "root");
            writer.push(1, "child");
            writer.push(2, "grandchild");

            //then
            assertEquals("root\n child\n  grandchild", writer.toString());
        }

        @Test
        @DisplayName("should handle indentation size 8")
        void testIndentSize8() {
            //given
            LineWriter writer = new LineWriter(8);

            //when
            writer.push(0, "root");
            writer.push(1, "child");

            //then
            assertEquals("root\n        child", writer.toString());
        }

        @Test
        @DisplayName("should handle many lines")
        void testManyLines() {
            //given
            LineWriter writer = new LineWriter(2);

            //when
            for (int i = 0; i < 100; i++) {
                writer.push(0, "line" + i);
            }

            //then
            String result = writer.toString();
            assertTrue(result.startsWith("line0\nline1\nline2"));
            assertTrue(result.endsWith("line98\nline99"));
            assertEquals(100, result.split("\n").length); // 100 lines split into 100 parts
        }
    }
}
