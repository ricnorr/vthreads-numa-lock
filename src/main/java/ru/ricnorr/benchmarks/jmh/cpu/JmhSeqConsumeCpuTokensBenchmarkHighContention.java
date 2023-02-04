package ru.ricnorr.benchmarks.jmh.cpu;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class JmhSeqConsumeCpuTokensBenchmarkHighContention {

    @Param("0")
    public long beforeCpuTokens;

    @Param("0")
    public long inCpuTokens;

    @Param("0")
    public int totalActions;

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bench() {
        Blackhole.consumeCPU(beforeCpuTokens);
        for (int i = 0; i < totalActions; i++) {
            Blackhole.consumeCPU(inCpuTokens);
        }
    }

}
