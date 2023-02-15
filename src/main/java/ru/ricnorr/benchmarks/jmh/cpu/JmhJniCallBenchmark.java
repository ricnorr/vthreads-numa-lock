package ru.ricnorr.benchmarks.jmh.cpu;

import org.openjdk.jmh.annotations.*;
import ru.ricnorr.numa.locks.Utils;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class JmhJniCallBenchmark {

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int bench() {
        return Utils.getClusterID();
    }

}
