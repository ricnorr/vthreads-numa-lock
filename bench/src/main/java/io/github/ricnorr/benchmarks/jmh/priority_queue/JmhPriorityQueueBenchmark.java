package io.github.ricnorr.benchmarks.jmh.priority_queue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.github.ricnorr.benchmarks.BenchUtils;
import io.github.ricnorr.benchmarks.BenchmarkException;
import io.github.ricnorr.benchmarks.LockType;
import io.github.ricnorr.numa_locks.VthreadNumaLock;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class JmhPriorityQueueBenchmark {

  @Param("0")
  public int actionsCount;

  @Param("0")
  public int threads;

  @Param("")
  public String lockType;
  VthreadNumaLock lock;

  CyclicBarrier cyclicBarrier;

  List<Thread> threadList = new ArrayList<>();

  Set<Thread> carrierThreads = new HashSet<>();

  PriorityQueue<Integer> priorityQueue = new PriorityQueue<>();

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
    if (!lockType.equals(LockType.SYNCHRONIZED.toString())) {
      lock = BenchUtils.initLock(LockType.valueOf(lockType), threads);
    }
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
    priorityQueue.clear();
    threadList = new ArrayList<>();
    latenciesForEachThread.add(new ArrayList<>());
    final int benchmarkIteration = latenciesForEachThread.size() - 1;
    for (int i = 0; i < threads; i++) {
      latenciesForEachThread.get(benchmarkIteration).add(new ArrayList<>());
    }
    BenchUtils.pinVirtualThreadsToCores(Math.min(BenchUtils.CORES_CNT, threads));
    cyclicBarrier = new CyclicBarrier(threads);
    for (int i = 0; i < threads; i++) {
      ThreadFactory threadFactory;
      threadFactory = Thread.ofVirtual().factory();
      int finalI = i;
      var thread = threadFactory.newThread(
          () -> {
            List<Long> threadLatencyNanosec = new ArrayList<>();
            try {
              cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
              throw new BenchmarkException("Fail waiting barrier", e);
            }
            for (int j = 0; j < actionsCount / threads; j++) {
              Thread.yield();
              long startAcquireLockNanos = System.nanoTime();
              var obj = lock.lock();
              long lockAcquiredNanos = System.nanoTime();
              threadLatencyNanosec.add(lockAcquiredNanos - startAcquireLockNanos);
              Thread.yield();
              if (ThreadLocalRandom.current().nextBoolean()) {
                priorityQueue.add(ThreadLocalRandom.current().nextInt(0, 50_000));
              } else {
                priorityQueue.poll();
              }
              Thread.yield();
              lock.unlock(obj);
            }
            latenciesForEachThread.get(benchmarkIteration).set(finalI, threadLatencyNanosec);
          }
      );
      thread.setName("virtual-" + i);
      threadList.add(thread);
    }
    System.out.println("I pinned " + carrierThreads.size() + " carrier threads");
  }

  @org.openjdk.jmh.annotations.Benchmark
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
