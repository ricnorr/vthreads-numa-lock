package ru.ricnorr.benchmarks.jmh.cpu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import ru.ricnorr.benchmarks.BenchmarkException;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.Utils;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class JmhParConsumeCpuTokensBenchmark {

  @Param("0")
  public long beforeCpuTokens;

  @Param("0")
  public long inCpuTokens;

  @Param("0")
  public int actionsCount;

  @Param("0")
  public int threads;

  @Param("")
  public String lockType;

  @Param("false")
  public boolean yieldInCrit;

  @Param("1")
  public int yieldsBefore;

  List<Thread> threadList = new ArrayList<>();

  final Object obj = new Object();

  // первое измерение - замер бенча
  // второе измерение - номер потока
  // третье измерение - latency взятия блокировки
  List<List<List<Long>>> latenciesForEachThread = new ArrayList<>();

  @Setup(Level.Trial)
  public void init() {
    System.out.println("Get system property jdk.virtualThreadScheduler.parallelism=" +
        System.getProperty("jdk.virtualThreadScheduler.parallelism"));
    System.out.println("Get system property jdk.virtualThreadScheduler.maxPoolSize=" +
        System.getProperty("jdk.virtualThreadScheduler.maxPoolSize"));
    Utils.pinVirtualThreadsToCores(Math.min(threads, Utils.CORES_CNT));
  }

  @TearDown(Level.Invocation)
  public void writeLatencies() throws IOException {
    System.out.println("Write latencies");
    Path latenciesDirectory = Paths.get("latencies");
    if (Files.notExists(latenciesDirectory)) {
      Files.createDirectory(latenciesDirectory);
    }
    for (int iteration = 0; iteration < latenciesForEachThread.size(); iteration++) {
      var latenciesForIteration = latenciesForEachThread.get(iteration);
      for (int thread = 0; thread < threads; thread++) {
        var latenciesForThread =
            latenciesForIteration.get(thread).stream().map(Object::toString).collect(Collectors.joining("\n"));
        Path newFile = Paths.get(String.format("latencies/%d_%d.tmp", iteration, thread));
        if (!Files.exists(newFile)) {
          Files.writeString(newFile, latenciesForThread, StandardOpenOption.CREATE);
        } else {
          Files.writeString(newFile, latenciesForThread, StandardOpenOption.TRUNCATE_EXISTING);
        }
      }
    }
    System.out.println("End write latencies");
  }

  @Setup(Level.Invocation)
  public void prepare() {
    threadList = new ArrayList<>();
    latenciesForEachThread.add(new ArrayList<>());
    final int benchmarkIteration = latenciesForEachThread.size() - 1;
    for (int i = 0; i < threads; i++) {
      latenciesForEachThread.get(benchmarkIteration).add(new ArrayList<>());
    }
    var cyclicBarrier = new CyclicBarrier(threads);
    var threadLatencyNanosec = new ArrayList<Long>();
    var lock = Utils.initLock(LockType.valueOf(lockType), threads);
    for (int i = 0; i < threads; i++) {
      ThreadFactory threadFactory;
      threadFactory = Thread.ofVirtual().factory();
      int finalI = i;
      var thread = threadFactory.newThread(
          () -> {
            try {
              cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
              throw new BenchmarkException("Fail waiting barrier", e);
            }
            Object nodeForLock = null;
            var work = actionsCount / threads;
            if (finalI == threads - 1) {
              work += actionsCount % threads;
            }
            for (int i1 = 0; i1 < work; i1++) {
              for (int i2 = 0; i2 < Math.max(yieldsBefore, 1); i2++) {
                Blackhole.consumeCPU(beforeCpuTokens / yieldsBefore);
                Thread.yield();
              }
              long startAcquireLockNanos = System.nanoTime();
              if (lockType.equals("SYNCHRONIZED")) {
                synchronized (obj) {
                  long lockAcquiredNanos = System.nanoTime();
                  threadLatencyNanosec.add(lockAcquiredNanos - startAcquireLockNanos);
                  Blackhole.consumeCPU(inCpuTokens);
                }
              } else {
                if (lock.canUseNodeFromPreviousLocking()) {
                  nodeForLock = lock.lock(nodeForLock);
                } else {
                  nodeForLock = lock.lock(null);
                }
                long lockAcquiredNanos = System.nanoTime();
                threadLatencyNanosec.add(lockAcquiredNanos - startAcquireLockNanos);
                Blackhole.consumeCPU(inCpuTokens);
                if (yieldInCrit) {
                  Thread.yield();
                }
                lock.unlock(nodeForLock);
              }
            }
            latenciesForEachThread.get(benchmarkIteration).set(finalI, threadLatencyNanosec);
          }
      );
      thread.setName("virtual-" + i);
      threadList.add(thread);
    }
//    System.out.println("I pinned " + carrierThreads.size() + " carrier threads");
  }

  @Benchmark
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
