package ru.ricnorr.benchmarks.jmh.trash;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
public class WeakCASBenchmark {


    public AtomicInteger atomicInt = new AtomicInteger(0);

    @Warmup(iterations = 1)
    @Fork(1)
    @Measurement(iterations = 5)
    @Benchmark
    @Threads(24)
    public void strongCas() {
        for (int i = 0; i < 10000; i++) {
            int x = atomicInt.get();
            while (!atomicInt.compareAndSet(x, x + 1)) {
                x = atomicInt.get();
            }
        }
    }

    @Warmup(iterations = 1)
    @Benchmark
    @Fork(1)
    @Threads(24)
    @Measurement(iterations = 5)
    public void weakCas() {
        for (int i = 0; i < 10000; i++) {
            int prev = atomicInt.get();
            while (!atomicInt.weakCompareAndSetVolatile(prev, prev + 1)) {
                prev = atomicInt.get();
            }
        }
    }
}

/**
 * strongCAS - thrpt - 366.174
 * weakCAS - thrpt - 352.996
 */
