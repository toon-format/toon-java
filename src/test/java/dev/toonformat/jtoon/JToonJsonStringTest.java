package dev.toonformat.jtoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
            String json = "{\"id\":123,\"name\":\"Ada\"}";

            // When
            String result = JToon.encodeJson(json);

            // Then
            assertEquals("id: 123\nname: Ada", result);
        }

        @Test
        @DisplayName("encodes primitive array inline")
        void encodesPrimitiveArray() {
            // Given
            String json = "{\"tags\":[\"admin\",\"ops\",\"dev\"]}";

            // When
            String result = JToon.encodeJson(json);

            // Then
            assertEquals("tags[3]: admin,ops,dev", result);
        }

        @Test
        @DisplayName("encodes uniform objects as tabular array")
        void encodesTabularArray() {
            // Given
            String json = "{\"items\":[{\"sku\":\"A1\",\"qty\":2,\"price\":9.99},{\"sku\":\"B2\",\"qty\":1,\"price\":14.5}]}";

            // When
            String result = JToon.encodeJson(json);

            // Then
            String expected = String.join("\n",
                    "items[2]{sku,qty,price}:",
                    "  A1,2,9.99",
                    "  B2,1,14.5");
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("encodes root-level array mixing primitive, object, and array of objects in list format")
        void encodesMixedArray() {
            // Given
            String json = "[\"summary\", { \"id\": 1, \"name\": \"Ada\" }, [{ \"id\": 2 }, { \"status\": \"draft\" }]]";

            // When
            String result = JToon.encodeJson(json);

            // Then
            String expected = String.join("\n",
                    "[3]:",
                    "  - summary",
                    "  - id: 1",
                    "    name: Ada",
                    "  - [2]:",
                    "    - id: 2",
                    "    - status: draft");
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("supports custom options with pipe delimiter and length marker")
        void encodesWithCustomOptions() {
            // Given
            String json = "{\"tags\":[\"reading\",\"gaming\",\"coding\"],\"items\":[{\"sku\":\"A1\",\"qty\":2,\"price\":9.99},{\"sku\":\"B2\",\"qty\":1,\"price\":14.5}]}";
            EncodeOptions options = new EncodeOptions(2, Delimiter.PIPE, true, KeyFolding.OFF, Integer.MAX_VALUE);

            // When
            String result = JToon.encodeJson(json, options);

            // Then
            String expected = String.join("\n",
                    "tags[#3|]: reading|gaming|coding",
                    "items[#2|]{sku|qty|price}:",
                    "  A1|2|9.99",
                    "  B2|1|14.5");
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("supports custom options in flatten")
        void encodesWithCustomFlattingOptions() {
            // Given
            String json = "{\n" +
                    "        \"a\": {\n" +
                    "          \"b\": {\n" +
                    "            \"c\": {\n" +
                    "              \"d\": 1\n" +
                    "            }\n" +
                    "          }\n" +
                    "        }\n" +
                    "      }";
            EncodeOptions options = EncodeOptions.withFlattenDepth(2);

            // When
            String result = JToon.encodeJson(json, options);

            // Then
            String expected = String.join("\n",
                    "a.b:",
                    "  c:",
                    "    d: 1");
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


