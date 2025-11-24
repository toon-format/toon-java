package dev.toonformat.jtoon.encoder;


import dev.toonformat.jtoon.Delimiter;
import dev.toonformat.jtoon.EncodeOptions;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for ObjectEncoder.encodeObject
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {
        "-Xms1G",
        "-Xmx1G",
        "-XX:+UseG1GC"
})
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
public class ObjectEncoderBenchmark {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Param({"small", "medium", "large"})
    public String size;

    @Param({"true", "false"})
    public boolean flatten;


    @Param({"1", "3", "6"})
    public int buildDepth;

    private ObjectNode payload;
    private EncodeOptions options;

    @Setup(Level.Trial)
    public void setup() {
        int sizeCount;
        switch (size) {
            case "small" -> sizeCount = 2;
            case "large" -> sizeCount = 200;
            case "medium" -> sizeCount = 20;
            default -> sizeCount = 20;
        }

        payload = createNestedObject(sizeCount, buildDepth);
        options = buildEncodeOptions(flatten);
    }

    /**
     * The benchmarked method: encodeObject must not re-use the same LineWriter instance across iterations,
     * so we create a fresh writer per invocation here.
     */
    @Benchmark
    public void benchEncodeObjectSingle(Blackhole bh) {
        LineWriter localWriter = new LineWriter(options.indent());

        ObjectEncoder.encodeObject(payload, localWriter, 0, options, null, null, null, Collections.synchronizedSet(new LinkedHashSet<>()));
        // consume the result so JIT can't optimize it away
        bh.consume(localWriter.toString());
    }

    private static ObjectNode createNestedObject(int elementsPerLevel, int depth) {
        ObjectNode root = MAPPER.createObjectNode();
        for (int i = 0; i < elementsPerLevel; i++) {
            String key = "key" + i;
            if (depth <= 1) {
                // leaf value - mix primitives/arrays/objects for realism
                if (i % 7 == 0) {
                    ArrayNode arr = MAPPER.createArrayNode();
                    arr.add(i);
                    arr.add("value-" + i);
                    root.set(key, arr);
                } else if (i % 5 == 0) {
                    root.put(key, true);
                } else {
                    root.put(key, "value-" + i);
                }
            } else {
                // nested object
                root.set(key, createNestedObject(Math.max(1, elementsPerLevel / 4), depth - 1));
            }
        }
        return root;
    }

    /**
     * Build an EncodeOptions instance for your project.
     */
    private static EncodeOptions buildEncodeOptions(boolean flattenEnabled) {
        return new EncodeOptions(4, Delimiter.COMMA, false, flattenEnabled, 5);
    }
}
