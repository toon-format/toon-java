package dev.toonformat.jtoon.conformance;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import dev.toonformat.jtoon.DecodeOptions;
import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.EncodeOptions;
import dev.toonformat.jtoon.JToon;
import dev.toonformat.jtoon.KeyFolding;
import dev.toonformat.jtoon.PathExpansion;
import dev.toonformat.jtoon.conformance.model.DecodeTestFixture;
import dev.toonformat.jtoon.conformance.model.EncodeTestFixture;
import dev.toonformat.jtoon.conformance.model.JsonDecodeTestCase;
import dev.toonformat.jtoon.conformance.model.JsonDecodeTestOptions;
import dev.toonformat.jtoon.conformance.model.JsonEncodeTestCase;
import dev.toonformat.jtoon.conformance.model.JsonEncodeTestOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import tools.jackson.databind.ObjectMapper;

@Tag("unit")
public class ConformanceTest {
    @Nested
    @DisplayName("Encoding conformance tests")
    class EncodeJsonTest {
        private final ObjectMapper mapper = new ObjectMapper();

        @TestFactory
        Stream<DynamicNode> testJsonFile() {
            final File directory = new File("src/test/resources/conformance/encode");
            return loadTestFixtures(directory)
                .map(this::createTestContainer);
        }

        private Stream<EncodeTestFile> loadTestFixtures(final File directory) {
            final File[] files = Objects.requireNonNull(directory.listFiles());
            return Arrays.stream(files)
                .map(this::parseFixture);
        }

        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
        private EncodeTestFile parseFixture(final File file) {
            try {
                final EncodeTestFixture fixture = mapper.readValue(file, EncodeTestFixture.class);
                return new EncodeTestFile(file, fixture);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to parse test fixture: " + file.getName(), exception);
            }
        }

        private DynamicContainer createTestContainer(final EncodeTestFile encodeFile) {
            final File file = encodeFile.file();
            final Stream<DynamicTest> tests = createTestsFromFixture(encodeFile);

            return DynamicContainer.dynamicContainer(
                file.getName(),
                tests);
        }

        private Stream<DynamicTest> createTestsFromFixture(final EncodeTestFile encodeFile) {
            final EncodeTestFixture fixture = encodeFile.fixture();
            return fixture.tests().stream()
                .map(this::createDynamicTest);
        }

        private DynamicTest createDynamicTest(final JsonEncodeTestCase testCase) {
            return DynamicTest.dynamicTest(testCase.name(), () -> executeTestCase(testCase));
        }

        private void executeTestCase(final JsonEncodeTestCase testCase) {
            final EncodeOptions options = parseOptions(testCase.options());
            final String jsonInput = mapper.writeValueAsString(testCase.input());
            final String actual = JToon.encodeJson(jsonInput, options);
            assertEquals(testCase.expected(), actual);
        }

        private EncodeOptions parseOptions(final JsonEncodeTestOptions options) {
            if (options == null) {
                return EncodeOptions.DEFAULT;
            }

            final int indent = options.indent() != null ? options.indent() : 2;

            Delimiter delimiter = Delimiter.COMMA;
            if (options.delimiter() != null) {
                final String delimiterValue = options.delimiter();
                delimiter = switch (delimiterValue) {
                    case "\t" -> Delimiter.TAB;
                    case "|" -> Delimiter.PIPE;
                    case "," -> Delimiter.COMMA;
                    default -> delimiter;
                };
            }

            final boolean lengthMarker = options.lengthMarker() != null && "#".equals(options.lengthMarker());
            final KeyFolding flatten = options.keyFolding() != null && options.keyFolding().equals("safe") ?
                KeyFolding.SAFE :
                KeyFolding.OFF;
            final int depth = options.flattenDepth() != null ? options.flattenDepth() : Integer.MAX_VALUE;
            return new EncodeOptions(indent, delimiter, lengthMarker, flatten, depth);
        }

        private record EncodeTestFile(File file, EncodeTestFixture fixture) {
        }
    }

    @Nested
    @DisplayName("Decoding conformance tests")
    class DecodeJsonTest {
        private final ObjectMapper mapper = new ObjectMapper();

        @TestFactory
        Stream<DynamicNode> testJsonFile() {
            final File directory = new File("src/test/resources/conformance/decode");
            return loadTestFixtures(directory)
                .map(this::createTestContainer);
        }

        private Stream<DecodeTestFile> loadTestFixtures(final File directory) {
            final File[] files = Objects.requireNonNull(directory.listFiles());
            return Arrays.stream(files)
                .map(this::parseFixture);
        }

        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
        private DecodeTestFile parseFixture(final File file) {
            try {
                final DecodeTestFixture fixture = mapper.readValue(file, DecodeTestFixture.class);
                return new DecodeTestFile(file, fixture);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to parse test fixture: " + file.getName(), exception);
            }
        }

        private DynamicContainer createTestContainer(final DecodeTestFile decodeFile) {
            final File file = decodeFile.file();
            final Stream<DynamicTest> tests = createTestsFromFixture(decodeFile);

            return DynamicContainer.dynamicContainer(
                file.getName(),
                tests);
        }

        private Stream<DynamicTest> createTestsFromFixture(final DecodeTestFile decodeFile) {
            final DecodeTestFixture fixture = decodeFile.fixture();
            return fixture.tests().stream()
                .map(this::createDynamicTest);
        }

        private DynamicTest createDynamicTest(final JsonDecodeTestCase testCase) {
            return DynamicTest.dynamicTest(testCase.name(), () -> executeTestCase(testCase));
        }

        private void executeTestCase(final JsonDecodeTestCase testCase) {
            final DecodeOptions options = parseOptions(testCase.options());
            final String toonInput = testCase.input().asString();

            if (Boolean.TRUE.equals(testCase.shouldError())) {
                final Object actual;
                try {
                    actual = JToon.decode(toonInput, options);
                } catch (IllegalArgumentException e) {
                    return;
                }
                final String actualJson = mapper.writeValueAsString(actual);
                fail("Expected IllegalArgumentException but got result: " + actualJson);
            } else {
                final Object actual = JToon.decode(toonInput, options);
                if (testCase.expected() == null || testCase.expected().isNull()) {
                    assertNull(actual, "Expected null but got: " + actual);
                } else {
                    final String actualJson = mapper.writeValueAsString(actual);
                    final String expectedJson = mapper.writeValueAsString(testCase.expected());
                    assertEquals(expectedJson, actualJson);
                }
            }
        }

        private DecodeOptions parseOptions(final JsonDecodeTestOptions options) {
            if (options == null) {
                return DecodeOptions.DEFAULT;
            }

            final int indent = options.indent() != null ? options.indent() : 2;

            Delimiter delimiter = Delimiter.COMMA;
            if (options.delimiter() != null) {
                final String delimiterValue = options.delimiter();
                delimiter = switch (delimiterValue) {
                    case "\t" -> Delimiter.TAB;
                    case "|" -> Delimiter.PIPE;
                    case "," -> Delimiter.COMMA;
                    default -> delimiter;
                };
            }

            final boolean strict = options.strict() != null ? options.strict() : true;

            PathExpansion expandPaths = null;
            if (options.expandPaths() != null) {
                expandPaths = switch (options.expandPaths().toLowerCase(Locale.ROOT)) {
                    case "safe" -> PathExpansion.SAFE;
                    default -> PathExpansion.OFF;
                };
            }

            return new DecodeOptions(indent, delimiter, strict, expandPaths,
                DecodeOptions.MAX_ALLOWED_DEPTH, DecodeOptions.DEFAULT_MAX_ARRAY_SIZE,
                DecodeOptions.DEFAULT_MAX_STRING_LENGTH);
        }

        private record DecodeTestFile(File file, DecodeTestFixture fixture) {
        }
    }
}
