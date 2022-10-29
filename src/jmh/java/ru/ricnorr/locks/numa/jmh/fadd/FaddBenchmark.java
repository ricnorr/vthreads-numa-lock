package ru.ricnorr.locks.numa.jmh.fadd;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import ru.ricnorr.locks.numa.jmh.BenchmarkState;
import ru.ricnorr.locks.numa.jmh.BenchmarkUtil;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.Lock;

/**
 * Benchmark idea from <a href="https://onlinelibrary.wiley.com/doi/abs/10.1002/cpe.5964">...</a>
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class FaddBenchmark {
    @State(Scope.Benchmark) // All threads share this state
    public static class MultiFaddState extends BenchmarkState {

        public int multiVariableCount = 4;

        public AtomicIntegerArray multiVariableArray;

        public Lock lock;

        @Setup(Level.Trial)
        public void setUp() {
            lock = BenchmarkUtil.initLock(lockType);
            multiVariableArray = new AtomicIntegerArray(multiVariableCount);
        }
    }

    @Benchmark
    public void multiVar(Blackhole bh, MultiFaddState state) {
        state.lock.lock();
        int index = ThreadLocalRandom.current().nextInt(0, state.multiVariableCount);
        bh.consume(state.multiVariableArray.getAndAdd(index, 1));
        state.lock.unlock();
    }

    @State(Scope.Benchmark) // All threads share this state
    public static class SingleVariableBenchmarkState extends BenchmarkState {

        public AtomicInteger variable;

        public Lock lock;

        @Setup(Level.Trial)
        public void setUp() {
            lock = BenchmarkUtil.initLock(lockType);
            variable = new AtomicInteger();
        }
    }

    @Benchmark
    public void singleVar(Blackhole bh, SingleVariableBenchmarkState state) {
        state.lock.lock();
        bh.consume(state.variable.getAndAdd(1));
        state.lock.unlock();
    }
}
