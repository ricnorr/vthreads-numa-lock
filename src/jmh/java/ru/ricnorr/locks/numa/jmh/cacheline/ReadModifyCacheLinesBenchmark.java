package ru.ricnorr.locks.numa.jmh.cacheline;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import ru.ricnorr.locks.numa.jmh.BenchmarkUtil;
import ru.ricnorr.locks.numa.jmh.LockType;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ReadModifyCacheLinesBenchmark {
    @State(Scope.Benchmark) // All threads share this state
    public static class CacheLineState {

        @Param
        public LockType lockType;

        @Param({"32", "64", "128"})
        public int cacheLineSize;

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
