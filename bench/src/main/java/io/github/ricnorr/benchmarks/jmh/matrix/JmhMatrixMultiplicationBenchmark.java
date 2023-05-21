package io.github.ricnorr.benchmarks.jmh.matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import io.github.ricnorr.benchmarks.BenchUtils;
import io.github.ricnorr.benchmarks.BenchmarkException;
import io.github.ricnorr.benchmarks.LockType;
import org.ejml.concurrency.EjmlConcurrency;
import org.ejml.simple.SimpleMatrix;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class JmhMatrixMultiplicationBenchmark {

  @Param("0")
  public int beforeMatrixSize;
  @Param("0")
  public int inMatrixSize;
  @Param("0")
  public int actionsCount;
  @Param("0")
  public int threads;
  @Param("")
  public String lockType;
  @Param("false")
  public boolean yieldInCrit;
  List<Thread> threadList = new ArrayList<>();

  Phaser phaser;

  @Setup(Level.Trial)
  public void init() {
    System.out.println("Get system property jdk.virtualThreadScheduler.parallelism=" +
        System.getProperty("jdk.virtualThreadScheduler.parallelism"));
    System.out.println("Get system property jdk.virtualThreadScheduler.maxPoolSize=" +
        System.getProperty("jdk.virtualThreadScheduler.maxPoolSize"));
    EjmlConcurrency.USE_CONCURRENT = false;
    BenchUtils.pinVirtualThreadsToCores(Math.min(threads, BenchUtils.CORES_CNT));
  }

  @Setup(Level.Invocation)
  public void prepare() {
    threadList = new ArrayList<>();
    var cyclicBarrier = new CyclicBarrier(threads);
    var lock = BenchUtils.initLock(LockType.valueOf(lockType), threads);
    ThreadFactory threadFactory = Thread.ofVirtual().factory();
    phaser = new Phaser(threads + 1);
    for (int i = 0; i < threads; i++) {
      int finalI = i;
      var thread = threadFactory.newThread(
          () -> {
            SimpleMatrix beforeA =
                SimpleMatrix.random_DDRM(beforeMatrixSize, beforeMatrixSize, Double.MIN_VALUE, Double.MAX_VALUE,
                    ThreadLocalRandom.current());
            SimpleMatrix beforeB =
                SimpleMatrix.random_DDRM(beforeMatrixSize, beforeMatrixSize, Double.MIN_VALUE, Double.MAX_VALUE,
                    ThreadLocalRandom.current());
            SimpleMatrix inA =
                SimpleMatrix.random_DDRM(inMatrixSize, inMatrixSize, Double.MIN_VALUE, Double.MAX_VALUE,
                    ThreadLocalRandom.current());
            SimpleMatrix inB =
                SimpleMatrix.random_DDRM(inMatrixSize, inMatrixSize, Double.MIN_VALUE, Double.MAX_VALUE,
                    ThreadLocalRandom.current());
            try {
              cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
              throw new BenchmarkException("Fail waiting barrier", e);
            }
            var work = actionsCount / threads;
            if (finalI == threads - 1) {
              work += actionsCount % threads;
            }
            for (int i1 = 0; i1 < work; i1++) {
              beforeA = beforeA.mult(beforeB);
              Thread.yield();
              var nodeForLock = lock.lock();
              inA = inA.mult(inB);
              if (yieldInCrit) {
                Thread.yield();
              }
              lock.unlock(nodeForLock);
            }
            phaser.arrive();
          }
      );
      thread.setName("virtual-" + i);
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
    phaser.arriveAndAwaitAdvance();
  }
}
