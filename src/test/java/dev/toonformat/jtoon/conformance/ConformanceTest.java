package dev.toonformat.jtoon.conformance;

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

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class ConformanceTest {
    @Nested
    @DisplayName("Encoding conformance tests")
    class encodeJsonTest {
        private final ObjectMapper mapper = new ObjectMapper();

        @TestFactory
        Stream<DynamicNode> testJSONFile() {
            File directory = new File("src/test/resources/conformance/encode");
            return loadTestFixtures(directory)
                .map(this::createTestContainer);
        }

        private Stream<EncodeTestFile> loadTestFixtures(File directory) {
            File[] files = Objects.requireNonNull(directory.listFiles());
            return Arrays.stream(files)
                .map(this::parseFixture);
        }

        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
        private EncodeTestFile parseFixture(File file) {
            try {
                EncodeTestFixture fixture = mapper.readValue(file, EncodeTestFixture.class);
                return new EncodeTestFile(file, fixture);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to parse test fixture: " + file.getName(), exception);
            }
        }

        private DynamicContainer createTestContainer(EncodeTestFile encodeFile) {
            File file = encodeFile.file();
            Stream<DynamicTest> tests = createTestsFromFixture(encodeFile);

            return DynamicContainer.dynamicContainer(
                file.getName(),
                tests);
        }

        private Stream<DynamicTest> createTestsFromFixture(EncodeTestFile encodeFile) {
            EncodeTestFixture fixture = encodeFile.fixture();
            return fixture.tests().stream()
                .map(this::createDynamicTest);
        }

        private DynamicTest createDynamicTest(JsonEncodeTestCase testCase) {
            return DynamicTest.dynamicTest(testCase.name(), () -> executeTestCase(testCase));
        }

        private void executeTestCase(JsonEncodeTestCase testCase) {
            EncodeOptions options = parseOptions(testCase.options());
            String jsonInput = mapper.writeValueAsString(testCase.input());
            String actual = JToon.encodeJson(jsonInput, options);
            assertEquals(testCase.expected(), actual);
        }

        private EncodeOptions parseOptions(JsonEncodeTestOptions options) {
            if (options == null) {
                return EncodeOptions.DEFAULT;
            }

            int indent = options.indent() != null ? options.indent() : 2;

            Delimiter delimiter = Delimiter.COMMA;
            if (options.delimiter() != null) {
                String delimiterValue = options.delimiter();
                delimiter = switch (delimiterValue) {
                    case "\t" -> Delimiter.TAB;
                    case "|" -> Delimiter.PIPE;
                    case "," -> Delimiter.COMMA;
                    default -> delimiter;
                };
            }

            boolean lengthMarker = options.lengthMarker() != null && "#".equals(options.lengthMarker());
            KeyFolding flatten = options.keyFolding() != null && options.keyFolding().equals("safe") ?
                KeyFolding.SAFE :
                KeyFolding.OFF;
            int depth = options.flattenDepth() != null ? options.flattenDepth() : Integer.MAX_VALUE;
            return new EncodeOptions(indent, delimiter, lengthMarker, flatten, depth);
        }

        private record EncodeTestFile(File file, EncodeTestFixture fixture) {
        }
    }

    @Nested
    @DisplayName("Decoding conformance tests")
    class decodeJsonTest {
        private final ObjectMapper mapper = new ObjectMapper();

        @TestFactory
        Stream<DynamicNode> testJSONFile() {
            File directory = new File("src/test/resources/conformance/decode");
            return loadTestFixtures(directory)
                .map(this::createTestContainer);
        }

        private Stream<DecodeTestFile> loadTestFixtures(File directory) {
            File[] files = Objects.requireNonNull(directory.listFiles());
            return Arrays.stream(files)
                .map(this::parseFixture);
        }

        @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
        private DecodeTestFile parseFixture(File file) {
            try {
                var fixture = mapper.readValue(file, DecodeTestFixture.class);
                return new DecodeTestFile(file, fixture);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to parse test fixture: " + file.getName(), exception);
            }
        }

        private DynamicContainer createTestContainer(DecodeTestFile decodeFile) {
            File file = decodeFile.file();
            Stream<DynamicTest> tests = createTestsFromFixture(decodeFile);

            return DynamicContainer.dynamicContainer(
                file.getName(),
                tests);
        }

        private Stream<DynamicTest> createTestsFromFixture(DecodeTestFile decodeFile) {
            var fixture = decodeFile.fixture();
            return fixture.tests().stream()
                .map(this::createDynamicTest);
        }

        private DynamicTest createDynamicTest(JsonDecodeTestCase testCase) {
            return DynamicTest.dynamicTest(testCase.name(), () -> executeTestCase(testCase));
        }

        private void executeTestCase(JsonDecodeTestCase testCase) {
            var options = parseOptions(testCase.options());
            String toonInput = testCase.input().asString();

            if (Boolean.TRUE.equals(testCase.shouldError())) {
                Object actual;
                try {
                    actual = JToon.decode(toonInput, options);
                } catch (IllegalArgumentException e) {
                    return;
                }
                String actualJson = mapper.writeValueAsString(actual);
                fail("Expected IllegalArgumentException but got result: " + actualJson);
            } else {
                Object actual = JToon.decode(toonInput, options);
                if (testCase.expected() == null || testCase.expected().isNull()) {
                    assertNull(actual, "Expected null but got: " + actual);
                } else {
                    String actualJson = mapper.writeValueAsString(actual);
                    String expectedJson = mapper.writeValueAsString(testCase.expected());
                    assertEquals(expectedJson, actualJson);
                }
            }
        }

        private DecodeOptions parseOptions(JsonDecodeTestOptions options) {
            if (options == null) {
                return DecodeOptions.DEFAULT;
            }

            int indent = options.indent() != null ? options.indent() : 2;

            Delimiter delimiter = Delimiter.COMMA;
            if (options.delimiter() != null) {
                String delimiterValue = options.delimiter();
                delimiter = switch (delimiterValue) {
                    case "\t" -> Delimiter.TAB;
                    case "|" -> Delimiter.PIPE;
                    case "," -> Delimiter.COMMA;
                    default -> delimiter;
                };
            }

            boolean strict = options.strict() != null ? options.strict() : true;

            PathExpansion expandPaths = null;
            if (options.expandPaths() != null) {
                expandPaths = switch (options.expandPaths().toLowerCase()) {
                    case "safe" -> PathExpansion.SAFE;
                    default -> PathExpansion.OFF;
                };
            }

            return new DecodeOptions(indent, delimiter, strict, expandPaths);
        }

        private record DecodeTestFile(File file, DecodeTestFixture fixture) {
        }
    }
}
