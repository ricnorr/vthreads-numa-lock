package ru.ricnorr.locks.numa.jmh.cacheline;

import org.openjdk.jmh.annotations.*;
import ru.ricnorr.locks.numa.jmh.BenchmarkState;
import ru.ricnorr.locks.numa.jmh.BenchmarkUtil;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ReadModifyCacheLinesBenchmark {
    @State(Scope.Benchmark) // All threads share this state
    public static class CacheLineState extends BenchmarkState {

        public int cacheLineSize = 32;

        public byte[] cacheLine1 = new byte[cacheLineSize];
        public byte[] cacheLine2 = new byte[cacheLineSize];
        public byte[] cacheLine3 = new byte[cacheLineSize];
        public byte[] cacheLine4 = new byte[cacheLineSize];

        public Lock lock;

        @Setup(Level.Trial)
        public void setUp() {
            lock = BenchmarkUtil.initLock(lockType);
        }
    }

    @Benchmark
    @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public void cacheLine(CacheLineState state) {
        state.lock.lock();
        modifyWriteCacheline(state.cacheLine1);
        modifyWriteCacheline(state.cacheLine2);
        modifyWriteCacheline(state.cacheLine3);
        modifyWriteCacheline(state.cacheLine4);
        state.lock.unlock();
    }

    public void modifyWriteCacheline(byte[] cacheLine) {
        byte rand = (byte) ThreadLocalRandom.current().nextInt();
        Arrays.fill(cacheLine, rand);
    }
}
