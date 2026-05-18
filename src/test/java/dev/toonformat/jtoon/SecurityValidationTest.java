package dev.toonformat.jtoon;

import dev.toonformat.jtoon.normalizer.JsonNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityValidationTest {

    @Nested
    @DisplayName("EncodeOptions validation")
    class EncodeOptionsValidation {
        @Test
        @DisplayName("should reject negative indent")
        void testNegativeIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new EncodeOptions(-1, Delimiter.COMMA, false, KeyFolding.OFF, 10));
        }

        @Test
        @DisplayName("should reject indent exceeding MAX_INDENT")
        void testExcessiveIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new EncodeOptions(EncodeOptions.MAX_ALLOWED_INDENT + 1, Delimiter.COMMA, false, KeyFolding.OFF, 10));
        }

        @Test
        @DisplayName("should reject null delimiter")
        void testNullDelimiter() {
            assertThrows(NullPointerException.class,
                () -> new EncodeOptions(2, null, false, KeyFolding.OFF, 10));
        }

        @Test
        @DisplayName("should reject negative flattenDepth")
        void testNegativeFlattenDepth() {
            assertThrows(IllegalArgumentException.class,
                () -> new EncodeOptions(2, Delimiter.COMMA, false, KeyFolding.SAFE, -1));
        }

        @Test
        @DisplayName("should accept valid options")
        void testValidOptions() {
            EncodeOptions opts = new EncodeOptions(4, Delimiter.PIPE, true, KeyFolding.SAFE, 5);
            assertEquals(4, opts.indent());
            assertEquals(Delimiter.PIPE, opts.delimiter());
            assertEquals(5, opts.flattenDepth());
        }
    }

    @Nested
    @DisplayName("DecodeOptions validation")
    class DecodeOptionsValidation {
        @Test
        @DisplayName("should reject negative indent")
        void testNegativeIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new DecodeOptions(-1, Delimiter.COMMA, true, PathExpansion.OFF, DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE, DecodeOptions.DEFAULT_MAX_STRING_LENGTH));
        }

        @Test
        @DisplayName("should reject indent exceeding MAX_INDENT")
        void testExcessiveIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new DecodeOptions(DecodeOptions.MAX_ALLOWED_INDENT + 1, Delimiter.COMMA, true, PathExpansion.OFF, DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE, DecodeOptions.DEFAULT_MAX_STRING_LENGTH));
        }

        @Test
        @DisplayName("should reject null delimiter")
        void testNullDelimiter() {
            assertThrows(NullPointerException.class,
                () -> new DecodeOptions(2, null, true, PathExpansion.OFF, DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE, DecodeOptions.DEFAULT_MAX_STRING_LENGTH));
        }
    }

    @Nested
    @DisplayName("Decode depth limit enforcement")
    class DecodeDepthLimit {

        @Test
        @DisplayName("should decode safely nested TOON within depth limit")
        void testWithinDepthLimit() {
            // Given: TOON with 500 levels of nesting (within default 512 limit)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 500; i++) {
                sb.append("  ".repeat(i));
                if (i < 499) {
                    sb.append('a').append(i).append(":\n");
                } else {
                    sb.append('a').append(i).append(": value");
                }
            }
            String toon = sb.toString();

            // When
            Object result = JToon.decode(toon);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should reject TOON exceeding max depth")
        void testExceedsDepthLimit() {
            // Given: TOON with 600 levels of nesting (exceeds default 512 limit)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 600; i++) {
                sb.append("  ".repeat(i));
                if (i < 599) {
                    sb.append('a').append(i).append(":\n");
                } else {
                    sb.append('a').append(i).append(": value");
                }
            }
            String toon = sb.toString();

            // Then
            assertThrows(IllegalArgumentException.class, () -> JToon.decode(toon));
        }
    }

    @Nested
    @DisplayName("Decode array size enforcement")
    class DecodeArraySizeEnforcement {

        @Test
        @DisplayName("should reject array header exceeding maxArraySize")
        void testArrayExceedsMaxSize() {
            // Given: TOON with array header claiming 10,000,001 elements
            String toon = "items[10000001]: 1,2,3";

            // Then
            assertThrows(IllegalArgumentException.class, () -> JToon.decode(toon));
        }

        @Test
        @DisplayName("should allow array within maxArraySize")
        void testArrayWithinMaxSize() {
            // Given: TOON with array header within default limit
            String toon = "items[3]: 1,2,3";

            // When
            Object result = JToon.decode(toon);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should handle custom maxArraySize option")
        void testCustomMaxArraySize() {
            // Given: TOON with array size within custom limit
            DecodeOptions options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF, 512, 100, 10_000_000);
            String toon = "items[50]: " + "x,".repeat(49) + "x";

            // When
            Object result = JToon.decode(toon, options);

            // Then
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Decode string length enforcement")
    class DecodeStringLengthEnforcement {

        @Test
        @DisplayName("should reject string exceeding maxStringLength")
        void testStringExceedsMaxLength() {
            // Given: TOON with a string value of 10,000,001 characters
            String longValue = "x".repeat(10_000_001);
            String toon = "key: " + longValue;

            // Then (lenient mode to avoid error propagation overhead)
            assertThrows(IllegalArgumentException.class, () -> JToon.decode(toon));
        }

        @Test
        @DisplayName("should allow string within maxStringLength")
        void testStringWithinMaxLength() {
            // Given: TOON with a string value within default limit
            String toon = "key: hello";

            // When
            Object result = JToon.decode(toon);

            // Then
            assertEquals("hello", ((java.util.Map<?, ?>) result).get("key"));
        }
    }

    @Nested
    @DisplayName("Decode integer overflow prevention")
    class DecodeIntegerOverflowPrevention {

        @Test
        @DisplayName("should reject array header with size exceeding Integer.MAX_VALUE")
        void testOverflowArraySize() {
            // Given: TOON with array header size that would overflow Integer.parseInt
            String toon = "x[99999999999]: 1,2,3";

            // Then: should throw because 99999999999 > maxArraySize (10M)
            assertThrows(IllegalArgumentException.class, () -> JToon.decode(toon));
        }
    }

    @Nested
    @DisplayName("JsonNormalizer depth limits")
    class JsonNormalizerDepthLimits {
        @Test
        @DisplayName("should have MAX_DEPTH constant")
        void testMaxDepthConstant() {
            assertEquals(256, JsonNormalizer.MAX_ALLOWED_NESTING_DEPTH);
        }
    }
}
