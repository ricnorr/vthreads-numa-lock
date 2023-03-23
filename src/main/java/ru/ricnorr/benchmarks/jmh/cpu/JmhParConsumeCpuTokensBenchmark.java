package ru.ricnorr.benchmarks.jmh.cpu;

import com.sun.jna.Native;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import ru.ricnorr.benchmarks.BenchmarkException;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.Affinity;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jmh.annotations.Scope.Benchmark;
import static ru.ricnorr.benchmarks.Main.getProcessorsNumbersInNumaNodeOrder;
import static ru.ricnorr.benchmarks.Main.setAffinity;

@State(Benchmark)
public class JmhParConsumeCpuTokensBenchmark {

    @Param("false")
    public boolean isLightThread;

    @Param("0")
    public long beforeCpuTokens;

    @Param("0")
    public long inCpuTokens;

    @Param("0")
    public int actionsPerThread;

    @Param("0")
    public int threads;

    @Param("")
    public String lockType;

    @Param("")
    public String lockSpec;

    @Param("false")
    public boolean pinUsingJna;

    NumaLock lock;

    CyclicBarrier cyclicBarrier;

    List<Thread> threadList = new ArrayList<>();

    Set<Thread> carrierThreads = new HashSet<>();

    @Setup(Level.Trial)
    public void init() {
        if (!pinUsingJna && !System.getProperty("os.name").toLowerCase().contains("mac")) {
            List<Integer> processors = getProcessorsNumbersInNumaNodeOrder();
            setAffinity(threads, ProcessHandle.current().pid(), processors);
        }
        System.out.println("Get system property jdk.virtualThreadScheduler.parallelism=" +
                System.getProperty("jdk.virtualThreadScheduler.parallelism"));
        System.out.println("Get system property jdk.virtualThreadScheduler.maxPoolSize=" +
                System.getProperty("jdk.virtualThreadScheduler.maxPoolSize"));
        lock = Utils.initLock(LockType.valueOf(lockType));
    }

    @Setup(Level.Invocation)
    public void prepare() {
        threadList = new ArrayList<>();
        AtomicInteger customBarrier = new AtomicInteger();
        AtomicBoolean locked = new AtomicBoolean(false);
        cyclicBarrier = new CyclicBarrier(threads);
        for (int i = 0; i < threads; i++) {
            ThreadFactory threadFactory;
            if (isLightThread) {
                threadFactory = Thread.ofVirtual().factory();
            } else {
                threadFactory = Thread.ofPlatform().factory();
            }
            int finalI = i;
            threadList.add(threadFactory.newThread(
                    () -> {
                        if (pinUsingJna) {
                                customBarrier.incrementAndGet();
                                int cores = Math.min(threads, Runtime.getRuntime().availableProcessors());
                                while (customBarrier.get() < cores) {
                                }
                                while (!locked.compareAndSet(false, true)) {
                                }
                                Thread currentCarrier = Utils.getCurrentCarrierThread();
                                if (!carrierThreads.contains(currentCarrier)) {
                                    Affinity.affinityLib.pinToCore(carrierThreads.size());
                                    carrierThreads.add(currentCarrier);
                                }
                                locked.compareAndSet(true, false);
                        }
                        try {
                            cyclicBarrier.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            throw new BenchmarkException("Fail waiting barrier", e);
                        }
                        Object nodeForLock = null;
                        for (int i1 = 0; i1 < actionsPerThread; i1++) {
                            Blackhole.consumeCPU(beforeCpuTokens);
                            if (lock.canUseNodeFromPreviousLocking()) {
                                nodeForLock = lock.lock(nodeForLock);
                            } else {
                                nodeForLock = lock.lock(null);
                            }
                            Blackhole.consumeCPU(inCpuTokens);
                            lock.unlock(nodeForLock);
                        }
                    }
            ));
        }
        System.out.println("I pinned " + carrierThreads.size() + " carrier threads");
    }


    @Benchmark
    @Fork(1)
    @Warmup(iterations = 20)
    @Measurement(iterations = 20)
    @BenchmarkMode({Mode.SingleShotTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bench() {
        for (int i = 0; i < threads; i++) {
            threadList.get(i).start();
        }
        for (int i = 0; i < threads; i++) {
            try {
                threadList.get(i).join();
            } catch (InterruptedException e) {
                throw new BenchmarkException("Fail to join thread " + e.getMessage(), e);
            }
        }
    }
}
