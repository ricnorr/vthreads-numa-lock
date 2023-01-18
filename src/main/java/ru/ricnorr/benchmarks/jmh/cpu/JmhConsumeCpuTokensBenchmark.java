package ru.ricnorr.benchmarks.jmh.cpu;

import org.ejml.concurrency.EjmlConcurrency;
import org.ejml.simple.SimpleMatrix;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class JmhConsumeCpuTokensBenchmark {

    @Param("0")
    public long cpuTokens;

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void consumeCpu() {
        Blackhole.consumeCPU(cpuTokens);
    }
}
