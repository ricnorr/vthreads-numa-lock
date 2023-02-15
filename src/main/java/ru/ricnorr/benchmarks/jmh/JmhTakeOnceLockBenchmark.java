package ru.ricnorr.benchmarks.jmh;

import org.openjdk.jmh.annotations.*;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.benchmarks.Main;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class JmhTakeOnceLockBenchmark {
    @Param("")
    public String lockType;

    @Param("{}")
    public String lockSpec;

    Lock lock;

    @Setup
    public void init() {
        lock = Main.initLock(LockType.valueOf(lockType), lockSpec, false);
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bench() {
        lock.lock();
        lock.unlock();
    }
}
