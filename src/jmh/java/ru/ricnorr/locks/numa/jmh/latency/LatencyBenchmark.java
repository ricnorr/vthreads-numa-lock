package ru.ricnorr.locks.numa.jmh.latency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import ru.ricnorr.locks.numa.jmh.BenchmarkUtil;
import ru.ricnorr.locks.numa.jmh.LockType;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class LatencyBenchmark {
    @State(Scope.Benchmark) // All threads share this state
    public static class LatencyState {

        @Param
        public LockType lockType;

        public Lock lock;

        @Setup(Level.Trial)
        public void setUp() {
            lock = BenchmarkUtil.initLock(lockType);
        }

    }

    @Benchmark
    public void bench(Blackhole bh, LatencyState state) {
        state.lock.lock();
        bh.consume(null);
        state.lock.unlock();
    }
}
