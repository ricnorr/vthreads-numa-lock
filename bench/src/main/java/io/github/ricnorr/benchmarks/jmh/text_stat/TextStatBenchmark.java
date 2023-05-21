package io.github.ricnorr.benchmarks.jmh.text_stat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class TextStatBenchmark {

  @Param("0")
  public int threads;
  @Param("")
  public String lockType;
  List<Thread> threadList = new ArrayList<>();
  Phaser onFinish;

  @Setup(Level.Trial)
  public void init() {
    System.out.println("Get system property jdk.virtualThreadScheduler.parallelism=" +
        System.getProperty("jdk.virtualThreadScheduler.parallelism"));
    System.out.println("Get system property jdk.virtualThreadScheduler.maxPoolSize=" +
        System.getProperty("jdk.virtualThreadScheduler.maxPoolSize"));
    BenchUtils.pinVirtualThreadsToCores(Math.min(threads, BenchUtils.CORES_CNT));
  }

  @Setup(Level.Invocation)
  public void prepare() {
    threadList = new ArrayList<>();
    onFinish = new Phaser(threads + 1);
    var cyclicBarrier = new CyclicBarrier(threads);
    ThreadFactory threadFactory = Thread.ofVirtual().factory();
    byte[] array = new byte[256];
    int wordsCnt = 1_000_00;
    var words = new String[wordsCnt];
    for (int i = 0; i < wordsCnt; i++) {
      new Random().nextBytes(array);
      for (int j = 0; j < array.length - 20; j += 10) {
        array[j] = ' ';
      }
      words[i] = new String(array, StandardCharsets.UTF_8);
    }
    List<VthreadNumaLock> locks = new ArrayList<>();
    List<HashMap<String, Integer>> maps = new ArrayList<>();
    int cores = Math.min(BenchUtils.CORES_CNT, threads);
    for (int i = 0; i < cores; i++) {
      locks.add(BenchUtils.initLock(LockType.valueOf(lockType)));
      maps.add(new HashMap<>());
    }
    for (int i = 0; i < threads; i++) {
      int finalI = i;
      var thread = threadFactory.newThread(
          () -> {
            try {
              cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
              throw new BenchmarkException("Fail waiting barrier", e);
            }
            int wordsPerThread = wordsCnt / threads;
            int additionWork = 0;
            if (finalI == threads - 1) {
              additionWork += wordsCnt % threads;
            }
            for (int j = 0; j < wordsPerThread + additionWork; j++) {
              for (String x : words[finalI * wordsPerThread + j].split(" ")) {
                var hashCode = Math.abs(x.hashCode()) % cores;
                var obj = locks.get(hashCode).lock();
                maps.get(hashCode).put(x, maps.get(hashCode).getOrDefault(x, 0) + 1);
                locks.get(hashCode).unlock(obj);
              }
              Thread.yield();
            }
            onFinish.arrive();
          }
      );
      thread.setName("vt-thread-" + i);
      threadList.add(thread);
    }
  }

  @org.openjdk.jmh.annotations.Benchmark
  @BenchmarkMode({Mode.SingleShotTime})
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public void bench() {
    for (int i = 0; i < threads; i++) {
      threadList.get(i).start();
    }
    onFinish.arriveAndAwaitAdvance();
  }
}
