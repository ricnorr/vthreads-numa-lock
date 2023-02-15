package ru.ricnorr.benchmarks.jmh.matrix;

import org.ejml.concurrency.EjmlConcurrency;
import org.ejml.simple.SimpleMatrix;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import ru.ricnorr.benchmarks.BenchmarkException;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.benchmarks.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.openjdk.jmh.annotations.Scope.Benchmark;
import static ru.ricnorr.benchmarks.Main.getProcessorsNumbersInNumaNodeOrder;
import static ru.ricnorr.benchmarks.Main.setAffinity;

@State(Benchmark)
public class JmhParMatrixBenchmark {
    @Param("0")
    public int beforeSize;

    @Param("0")
    public int inSize;

    @Param("0")
    public int actionsPerThread;

    @Param("0")
    public int threads;

    @Param("")
    public String lockType;

    @Param("")
    public String lockSpec;

    SimpleMatrix beforeMatrixA;

    SimpleMatrix beforeMatrixB;

    SimpleMatrix inMatrixA;

    SimpleMatrix inMatrixB;

    Lock lock;

    @Setup
    public void init() {
        // Don't use concurrency
        EjmlConcurrency.USE_CONCURRENT = false;

        List<Integer> processors = getProcessorsNumbersInNumaNodeOrder();
        setAffinity(threads, ProcessHandle.current().pid(), processors);

        Random rand = new Random();
        beforeMatrixA = SimpleMatrix.random_DDRM(beforeSize, beforeSize, 0, Float.MAX_VALUE, rand);
        beforeMatrixB = SimpleMatrix.random_DDRM(beforeSize, beforeSize, 0, Float.MAX_VALUE, rand);

        inMatrixA = SimpleMatrix.random_DDRM(inSize, inSize, 0, Float.MAX_VALUE, rand);
        inMatrixB = SimpleMatrix.random_DDRM(inSize, inSize, 0, Float.MAX_VALUE, rand);


        lock = Main.initLock(LockType.valueOf(lockType), lockSpec, false);
    }


    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bench(Blackhole bh) {
        final CyclicBarrier ready = new CyclicBarrier(threads);
        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            threadList.add(new Thread(() -> {
                try {
                    ready.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new BenchmarkException("Fail waiting barrier", e);
                }
                for (int i1 = 0; i1 < actionsPerThread; i1++) {
                    bh.consume(beforeMatrixA.mult(beforeMatrixB));
                    lock.lock();
                    bh.consume(inMatrixA.mult(inMatrixB));
                    lock.unlock();
                }
            }));
        }
        for (int i = 0; i < threads; i++) {
            threadList.get(i).start();
        }
        for (int i = 0; i < threads; i++) {
            try {
                threadList.get(i).join();
            } catch (InterruptedException e) {
                throw new BenchmarkException("Fail to join thread", e);
            }
        }
    }
}
