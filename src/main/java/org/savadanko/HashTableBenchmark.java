package org.savadanko;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(value = 3, jvmArgsAppend = {"-Xms2g", "-Xmx2g"})
@State(Scope.Thread)
public class HashTableBenchmark {

    private static final int OPS = 1024;

//    @Param({"1000", "10000", "100000"})
//    public int size;
//
//    @Param({"0.50", "0.75", "0.90"})
//    public String load;

    @Param({"100000"})
    public int size;

    @Param({"0.75"})
    public String load;

    private float targetLoad;
    private Integer[] hitKeys;
    private Integer[] missKeys;

    private HashMap<Integer, Integer> jdk;
    private OpenHashTable<Integer, Integer> custom;
    private OpenHashTableOptimized<Integer, Integer> customOpt;

    @Setup(Level.Iteration)
    public void setup() {
        targetLoad = Float.parseFloat(load);

        int capacity = nextPowerOfTwo((int) Math.ceil(size / targetLoad));

        jdk = new HashMap<>((int) Math.ceil(size / targetLoad) + 1, targetLoad);

        custom = new OpenHashTable<>(capacity, false);

        customOpt = new OpenHashTableOptimized<>(capacity);

        for (int i = 0; i < size; i++) {
            Integer k = i;
            Integer v = i;
            jdk.put(k, v);
            custom.put(k, v);
            customOpt.put(k, v);
        }

        hitKeys = new Integer[OPS];
        missKeys = new Integer[OPS];

        int x = 1;
        for (int i = 0; i < OPS; i++) {
            x = x * 1664525 + 1013904223;
            int idx = (x & 0x7fffffff) % size;
            hitKeys[i] = idx;
            missKeys[i] = size + idx;
        }
    }

    // -------------------- GET HIT --------------------

    @Benchmark
    @OperationsPerInvocation(OPS)
    public void jdk_getHit(Blackhole bh) {
        for (int i = 0; i < OPS; i++) bh.consume(jdk.get(hitKeys[i]));
    }

    @Benchmark
    @OperationsPerInvocation(OPS)
    public void custom_getHit(Blackhole bh) {
        for (int i = 0; i < OPS; i++) bh.consume(custom.get(hitKeys[i]));
    }

    @Benchmark
    @OperationsPerInvocation(OPS)
    public void customOpt_getHit(Blackhole bh) {
        for (int i = 0; i < OPS; i++) bh.consume(customOpt.get(hitKeys[i]));
    }

    // -------------------- GET MISS --------------------

    @Benchmark
    @OperationsPerInvocation(OPS)
    public void jdk_getMiss(Blackhole bh) {
        for (int i = 0; i < OPS; i++) bh.consume(jdk.get(missKeys[i]));
    }

    @Benchmark
    @OperationsPerInvocation(OPS)
    public void custom_getMiss(Blackhole bh) {
        for (int i = 0; i < OPS; i++) bh.consume(custom.get(missKeys[i]));
    }

    @Benchmark
    @OperationsPerInvocation(OPS)
    public void customOpt_getMiss(Blackhole bh) {
        for (int i = 0; i < OPS; i++) bh.consume(customOpt.get(missKeys[i]));
    }

    // -------------------- PUT UPDATE (existing keys) --------------------

    @Benchmark
    @OperationsPerInvocation(OPS)
    public void jdk_putUpdate() {
        for (int i = 0; i < OPS; i++) jdk.put(hitKeys[i], -i);
    }

    @Benchmark
    @OperationsPerInvocation(OPS)
    public void custom_putUpdate() {
        for (int i = 0; i < OPS; i++) custom.put(hitKeys[i], -i);
    }

    @Benchmark
    @OperationsPerInvocation(OPS)
    public void customOpt_putUpdate() {
        for (int i = 0; i < OPS; i++) customOpt.put(hitKeys[i], -i);
    }

    // -------------------- utils --------------------

    private static int nextPowerOfTwo(int x) {
        int n = 1;
        while (n < x) n <<= 1;
        return n;
    }
}
