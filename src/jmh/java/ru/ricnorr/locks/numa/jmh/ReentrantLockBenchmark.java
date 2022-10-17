package ru.ricnorr.locks.numa.jmh;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ReentrantLockBenchmark {

    private static Lock lock = new ReentrantLock();

    private int action() {
        int counter = 0;
        for (int i = 0; i < 100; i++) {
            counter++;
        }
        return counter;
    }

    @Benchmark
    public void lockUnlock(Blackhole bh) {
        lock.lock();
        bh.consume(action());
        lock.unlock();
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(ReentrantLockBenchmark.class.getSimpleName())
            .forks(4)
            .threads(32)
            .build();
        new Runner(options).run();
    }
}

