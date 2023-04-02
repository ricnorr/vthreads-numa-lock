package ru.ricnorr.benchmarks.jmh.cpu;

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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import ru.ricnorr.benchmarks.BenchmarkException;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.Affinity;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

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

  @Param("false")
  public boolean yieldInCrit;

  @Param("1")
  public int yieldsBefore;

  NumaLock lock;

  CyclicBarrier cyclicBarrier;

  List<Thread> threadList = new ArrayList<>();

  Set<Thread> carrierThreads = new HashSet<>();

  final Object obj = new Object();

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
    if (!lockType.equals(LockType.SYNCHRONIZED.toString())) {
      lock = Utils.initLock(LockType.valueOf(lockType), threads);
    }
  }

  @Setup(Level.Invocation)
  public void prepare() {
    threadList = new ArrayList<>();
    AtomicInteger customBarrier = new AtomicInteger();
    AtomicBoolean locked = new AtomicBoolean(false);
    cyclicBarrier = new CyclicBarrier(threads);
    boolean isOversub = threads > Runtime.getRuntime().availableProcessors();
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
              for (int i2 = 0; i2 < Math.max(yieldsBefore, 1); i2++) {
                Blackhole.consumeCPU(beforeCpuTokens / yieldsBefore);
                Thread.yield();
              }
              if (lockType.equals("SYNCHRONIZED")) {
                synchronized (obj) {
                  Blackhole.consumeCPU(inCpuTokens);
                }
              } else {
                if (lock.canUseNodeFromPreviousLocking()) {
                  nodeForLock = lock.lock(nodeForLock);
                } else {
                  nodeForLock = lock.lock(null);
                }
                Blackhole.consumeCPU(inCpuTokens);
                if (yieldInCrit) {
                  Thread.yield();
                }
                lock.unlock(nodeForLock);
              }
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
