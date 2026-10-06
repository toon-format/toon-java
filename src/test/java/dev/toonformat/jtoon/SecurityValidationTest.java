package dev.toonformat.jtoon;

import static org.junit.jupiter.api.Assertions.*;
import dev.toonformat.jtoon.normalizer.JsonNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SecurityValidationTest {

    private static final int CUSTOM_INDENT = 4;
    private static final int CUSTOM_FLATTEN_DEPTH = 5;
    private static final int CUSTOM_MAX_ARRAY_SIZE = 100;

    @Nested
    @DisplayName("EncodeOptions validation")
    class EncodeOptionsValidation {
        @Test
        @DisplayName("should reject negative indent")
        void testNegativeIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new EncodeOptions(-1, Delimiter.COMMA, KeyFolding.OFF, CUSTOM_FLATTEN_DEPTH));
        }

        @Test
        @DisplayName("should reject indent exceeding MAX_INDENT")
        void testExcessiveIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new EncodeOptions(
                    EncodeOptions.MAX_ALLOWED_INDENT + 1, Delimiter.COMMA, KeyFolding.OFF, CUSTOM_FLATTEN_DEPTH));
        }

        @Test
        @DisplayName("should reject null delimiter")
        void testNullDelimiter() {
            assertThrows(NullPointerException.class,
                () -> new EncodeOptions(2, null, KeyFolding.OFF, CUSTOM_FLATTEN_DEPTH));
        }

        @Test
        @DisplayName("should reject negative flattenDepth")
        void testNegativeFlattenDepth() {
            assertThrows(IllegalArgumentException.class,
                () -> new EncodeOptions(2, Delimiter.COMMA, KeyFolding.SAFE, -1));
        }

        @Test
        @DisplayName("should accept valid options")
        void testValidOptions() {
            final EncodeOptions opts = new EncodeOptions(CUSTOM_INDENT, Delimiter.PIPE,
                    KeyFolding.SAFE, CUSTOM_FLATTEN_DEPTH);
            assertEquals(CUSTOM_INDENT, opts.indent());
            assertEquals(Delimiter.PIPE, opts.delimiter());
            assertEquals(CUSTOM_FLATTEN_DEPTH, opts.flattenDepth());
        }
    }

    @Nested
    @DisplayName("DecodeOptions validation")
    class DecodeOptionsValidation {
        @Test
        @DisplayName("should reject negative indent")
        void testNegativeIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new DecodeOptions(-1, Delimiter.COMMA, true, PathExpansion.OFF,
                        DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                        DecodeOptions.DEFAULT_MAX_STRING_LENGTH));
        }

        @Test
        @DisplayName("should reject indent exceeding MAX_INDENT")
        void testExcessiveIndent() {
            assertThrows(IllegalArgumentException.class,
                () -> new DecodeOptions(DecodeOptions.MAX_ALLOWED_INDENT + 1, Delimiter.COMMA, true,
                        PathExpansion.OFF, DecodeOptions.MAX_ALLOWED_DEPTH,
                        DecodeOptions.DEFAULT_MAX_ARRAY_SIZE, DecodeOptions.DEFAULT_MAX_STRING_LENGTH));
        }

        @Test
        @DisplayName("should reject null delimiter")
        void testNullDelimiter() {
            assertThrows(NullPointerException.class,
                () -> new DecodeOptions(2, null, true, PathExpansion.OFF,
                        DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                        DecodeOptions.DEFAULT_MAX_STRING_LENGTH));
        }
    }

    @Nested
    @DisplayName("Decode depth limit enforcement")
    class DecodeDepthLimit {

        @Test
        @DisplayName("should decode safely nested TOON within depth limit")
        void testWithinDepthLimit() {
            // Given: TOON with 500 levels of nesting (within default 512 limit)
            final int safeNestingLevels = 500;
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < safeNestingLevels; i++) {
                sb.append("  ".repeat(i));
                if (i < safeNestingLevels - 1) {
                    sb.append('a').append(i).append(":\n");
                } else {
                    sb.append('a').append(i).append(": value");
                }
            }
            final String toon = sb.toString();

            // When
            final Object result = JToon.decode(toon);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should reject TOON exceeding max depth")
        void testExceedsDepthLimit() {
            // Given: TOON with 600 levels of nesting (exceeds default 512 limit)
            final int exceededNestingLevels = 600;
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < exceededNestingLevels; i++) {
                sb.append("  ".repeat(i));
                if (i < exceededNestingLevels - 1) {
                    sb.append('a').append(i).append(":\n");
                } else {
                    sb.append('a').append(i).append(": value");
                }
            }
            final String toon = sb.toString();

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
            final String toon = "items[10000001]: 1,2,3";

            // Then
            assertThrows(IllegalArgumentException.class, () -> JToon.decode(toon));
        }

        @Test
        @DisplayName("should allow array within maxArraySize")
        void testArrayWithinMaxSize() {
            // Given: TOON with array header within default limit
            final String toon = "items[3]: 1,2,3";

            // When
            final Object result = JToon.decode(toon);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should handle custom maxArraySize option")
        void testCustomMaxArraySize() {
            // Given: TOON with array size within custom limit
            final DecodeOptions options = new DecodeOptions(2, Delimiter.COMMA, true, PathExpansion.OFF,
                    DecodeOptions.MAX_ALLOWED_DEPTH, CUSTOM_MAX_ARRAY_SIZE,
                    DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
            final String toon = "items[50]: " + "x,".repeat(49) + "x";

            // When
            final Object result = JToon.decode(toon, options);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should bound the actual element count in either mode")
        void testActualCountExceedsMaxSize() {
            for (final boolean strict : new boolean[]{true, false}) {
                final DecodeOptions options = new DecodeOptions(2, Delimiter.COMMA, strict, PathExpansion.OFF,
                        DecodeOptions.MAX_ALLOWED_DEPTH, 2, DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
                assertThrows(IllegalArgumentException.class, () -> JToon.decode("a[1]:\n  - 1\n  - 2\n  - 3", options));
                assertThrows(IllegalArgumentException.class, () -> JToon.decode("a[1]{x}:\n  1\n  2\n  3", options));
                assertThrows(IllegalArgumentException.class,
                    () -> JToon.decode("a[1:]{v}:\n  x: 1\n  y: 2\n  z: 3", options));
            }
        }
    }

    @Nested
    @DisplayName("Decode string length enforcement")
    class DecodeStringLengthEnforcement {

        @Test
        @DisplayName("should reject string exceeding maxStringLength")
        void testStringExceedsMaxLength() {
            // Given: TOON with a string value of 10,000,001 characters
            final String longValue = "x".repeat(10_000_001);
            final String toon = "key: " + longValue;

            // Then (lenient mode to avoid error propagation overhead)
            assertThrows(IllegalArgumentException.class, () -> JToon.decode(toon));
        }

        @Test
        @DisplayName("should allow string within maxStringLength")
        void testStringWithinMaxLength() {
            // Given: TOON with a string value within default limit
            final String toon = "key: hello";

            // When
            final Object result = JToon.decode(toon);

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
            final String toon = "x[99999999999]: 1,2,3";

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
            final int expectedMaxDepth = 256;
            assertEquals(expectedMaxDepth, JsonNormalizer.MAX_ALLOWED_NESTING_DEPTH);
        }
    }
}
