package ru.ricnorr.benchmarks.jmh.cpu;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class JmhSeqConsumeCpuTokensBenchmarkOversubscription {

    @Param("0")
    public long beforeCpuTokens;

    @Param("0")
    public long inCpuTokens;

    @Param("0")
    public int actionsPerThread;

    @Param("0")
    public int threads;

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bench() {
        Blackhole.consumeCPU(beforeCpuTokens);
        for (int i = 0; i < actionsPerThread * threads; i++) {
            Blackhole.consumeCPU(inCpuTokens);
        }
    }

}
