package ru.ricnorr.benchmarks.jmh.cpu;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class JmhConsumeCpuTokensBenchmark {

    @Param("0")
    public long cpuTokens;

    @Benchmark
    @Fork(1)
    @Warmup(iterations = 1, time = 5)
    @Measurement(iterations = 1, time = 5)
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void consumeCpu() {
        Blackhole.consumeCPU(cpuTokens);
    }
}
