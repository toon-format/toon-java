package dev.toonformat.jtoon;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("JToon.encodeJson - JSON string entry point")
public class JToonJsonStringTest {

    @Nested
    @DisplayName("happy paths")
    class HappyPaths {

        @Test
        @DisplayName("encodes simple object")
        void encodesSimpleObject() {
            // Given
            final String json = "{\"id\":123,\"name\":\"Ada\"}";

            // When
            final String result = JToon.encodeJson(json);

            // Then
            assertEquals("id: 123\nname: Ada", result);
        }

        @Test
        @DisplayName("encodes primitive array inline")
        void encodesPrimitiveArray() {
            // Given
            final String json = "{\"tags\":[\"admin\",\"ops\",\"dev\"]}";

            // When
            final String result = JToon.encodeJson(json);

            // Then
            assertEquals("tags[3]: admin,ops,dev", result);
        }

        @Test
        @DisplayName("encodes uniform objects as tabular array")
        void encodesTabularArray() {
            // Given
            final String json = "{\"items\":[{\"sku\":\"A1\",\"qty\":2,\"price\":9.99},"
                    + "{\"sku\":\"B2\",\"qty\":1,\"price\":14.5}]}";

            // When
            final String result = JToon.encodeJson(json);

            // Then
            final String expected = """
                    items[2]{sku,qty,price}:
                      A1,2,9.99
                      B2,1,14.5""";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("encodes root-level array mixing primitive, object, and array of objects in list format")
        void encodesMixedArray() {
            // Given
            final String json = "[\"summary\", { \"id\": 1, \"name\": \"Ada\" },"
                    + " [{ \"id\": 2 }, { \"status\": \"draft\" }]]";

            // When
            final String result = JToon.encodeJson(json);

            // Then
            final String expected = """
                    [3]:
                      - summary
                      - id: 1
                        name: Ada
                      - [2]:
                        - id: 2
                        - status: draft""";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("supports custom options with pipe delimiter and length marker")
        void encodesWithCustomOptions() {
            // Given
            final String json = "{\"tags\":[\"reading\",\"gaming\",\"coding\"],"
                    + "\"items\":[{\"sku\":\"A1\",\"qty\":2,\"price\":9.99},"
                    + "{\"sku\":\"B2\",\"qty\":1,\"price\":14.5}]}";
            final EncodeOptions options = new EncodeOptions(2, Delimiter.PIPE, true, KeyFolding.OFF, Integer.MAX_VALUE);

            // When
            final String result = JToon.encodeJson(json, options);

            // Then
            final String expected = """
                    tags[#3|]: reading|gaming|coding
                    items[#2|]{sku|qty|price}:
                      A1|2|9.99
                      B2|1|14.5""";
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("supports custom options in flatten")
        void encodesWithCustomFlattingOptions() {
            // Given
            final String json = """
                    {
                            "a": {
                              "b": {
                                "c": {
                                  "d": 1
                                }
                              }
                            }
                          }""";
            final EncodeOptions options = EncodeOptions.withFlattenDepth(2);

            // When
            final String result = JToon.encodeJson(json, options);

            // Then
            final String expected = """
                    a.b:
                      c:
                        d: 1""";
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("errors")
    class Errors {

        @Test
        @DisplayName("throws on invalid JSON")
        void throwsOnInvalidJson() {
            // Then
            assertThrows(IllegalArgumentException.class, () -> JToon.encodeJson("{invalid}"));
        }

        @Test
        @DisplayName("throws on blank JSON")
        void throwsOnBlankJson() {
            // Then
            assertThrows(IllegalArgumentException.class, () -> JToon.encodeJson("   \n \t  "));
        }
    }
}


