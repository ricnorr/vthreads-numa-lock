package ru.ricnorr.benchmarks.jmh.cpu;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class JmhSeqConsumeCpuTokensBenchmarkLowContention {

    @Param("0")
    public long beforeCpuTokens;

    @Param("0")
    public long inCpuTokens;

    @Param("0")
    public int actionsPerThread;

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bench() {
        for (int i = 0; i < actionsPerThread; i++) {
            Blackhole.consumeCPU(beforeCpuTokens);
            Blackhole.consumeCPU(inCpuTokens);
        }
    }

}
