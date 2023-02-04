package ru.ricnorr.benchmarks.jmh.lockunlock;

import org.openjdk.jmh.annotations.*;
import ru.ricnorr.benchmarks.BenchmarkException;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.benchmarks.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.openjdk.jmh.annotations.Scope.Benchmark;
import static ru.ricnorr.benchmarks.Main.getProcessorsNumbersInNumaNodeOrder;
import static ru.ricnorr.benchmarks.Main.setAffinity;

@State(Benchmark)
public class JmhLockUnlockBenchmark {

    @Param("false")
    public boolean isLightThread;

    @Param("0")
    public int actionsPerThread;

    @Param("0")
    public int threads;

    @Param("")
    public String lockType;

    @Param("")
    public String lockSpec;

    Lock lock;

    @Setup
    public void init() {
        if (!isLightThread && !System.getProperty("os.name").toLowerCase().contains("mac")) {
            List<Integer> processors = getProcessorsNumbersInNumaNodeOrder();
            setAffinity(threads, ProcessHandle.current().pid(), processors);
        }

        lock = Main.initLock(LockType.valueOf(lockType), lockSpec);
    }


    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bench() {
        final CyclicBarrier ready = new CyclicBarrier(threads);
        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Runnable runnable = () -> {
                try {
                    ready.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new BenchmarkException("Fail waiting barrier", e);
                }
                for (int i1 = 0; i1 < actionsPerThread; i1++) {
                    lock.lock();
                    lock.unlock();
                }
            };
            if (isLightThread) {
                threadList.add(Thread.ofVirtual().factory().newThread(runnable));
            } else {
                threadList.add(Thread.ofPlatform().factory().newThread(runnable));
            }
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
