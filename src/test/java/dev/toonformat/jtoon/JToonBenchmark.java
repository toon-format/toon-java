package dev.toonformat.jtoon;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
public class JToonBenchmark {

    @Param({"10", "100", "1000"})
    private int size;

    private Map<String, Object> testObject;
    private String toonString;
    private String jsonString;

    @Setup
    public void setup() {
        testObject = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Map<String, Object> nested = new HashMap<>();
            nested.put("id", i);
            nested.put("name", "item_" + i);
            nested.put("value", Math.random() * 1000);
            nested.put("active", i % 2 == 0);
            testObject.put("key_" + i, nested);
        }
        toonString = JToon.encode(testObject);
        jsonString = "{\"name\":\"test\",\"value\":42,\"items\":[" +
            String.join(",", java.util.Collections.nCopies(10, "{\"id\":1,\"name\":\"test\"}")) +
            "],\"nested\":{\"a\":1,\"b\":2,\"c\":3}}";
    }

    @Benchmark
    public String encodeObject() {
        return JToon.encode(testObject);
    }

    @Benchmark
    public String encodeJson() {
        return JToon.encodeJson(jsonString);
    }

    @Benchmark
    public Object decodeToon() {
        return JToon.decode(toonString);
    }

    @Benchmark
    public String decodeToonToJson() {
        return JToon.decodeToJson(toonString);
    }

    @Benchmark
    public Object decodeJson() {
        return JToon.decode(toonString);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(JToonBenchmark.class.getSimpleName())
            .result("build/jmh-results/results.json")
            .build();

        new Runner(opt).run();
    }
}
