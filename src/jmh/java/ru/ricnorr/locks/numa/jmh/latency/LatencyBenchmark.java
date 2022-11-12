package ru.ricnorr.locks.numa.jmh.latency;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import ru.ricnorr.locks.numa.jmh.BenchmarkState;
import ru.ricnorr.locks.numa.jmh.BenchmarkUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class LatencyBenchmark {
    @State(Scope.Benchmark) // All threads share this state
    public static class LatencyState extends BenchmarkState {

        public Lock lock;

        @Setup(Level.Trial)
        public void setUp() {
            lock = BenchmarkUtil.initLock(lockType);
        }

    }

    @Benchmark
    @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public void bench(Blackhole bh, LatencyState state) {
        state.lock.lock();
        bh.consume(null);
        state.lock.unlock();
    }
}
