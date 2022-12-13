package ru.ricnorr.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BenchmarkRunner {
    int iterations;

    public BenchmarkRunner(int iterations) {
        this.iterations = iterations;
    }

    private record IterationResult(int threads, double overhead, double throughput) {
    }

    private long measureOverhead(int threads, int actionsCount, Runnable runnable) {
        final CyclicBarrier ready = new CyclicBarrier(threads);
        List<Thread> threadList = new ArrayList<>();

        final int actionsForEachThread = actionsCount / threads;
        for (int i = 0; i < threads; i++) {
            threadList.add(new Thread(() -> {
                try {
                    ready.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new BenchmarkException("Fail waiting barrier", e);
                }
                for (int i1 = 0; i1 < actionsForEachThread; i1++) {
                    runnable.run();
                }
            }));
        }

        long beginNanos = System.nanoTime();
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
        long endNanos = System.nanoTime();
        return endNanos - beginNanos;
    }

    public BenchmarkResult benchmark(
        int threads,
        int actionsCount,
        Runnable actionWithLock,
        Runnable actionWithoutLock
    ) {
        List<IterationResult> iterationsResults = new ArrayList<>();
        System.out.println("Run iterations");
        for (int i = 0; i < iterations; i++) {
            System.out.println("Run iteration " + i);
            long withLockNanos = measureOverhead(threads, actionsCount, actionWithLock);
            long withoutLockNanos = measureOverhead(threads, actionsCount, actionWithoutLock);
            double overheadMillis = (withLockNanos - withoutLockNanos) / 1000.0;
            double throughput = actionsCount / (withLockNanos / 1000.0);

            iterationsResults.add(new IterationResult(threads, overheadMillis, throughput));
            System.out.println("End iteration " + i);
        }
        System.out.println("Benchmark completed");

        return new BenchmarkResult(iterationsResults.stream().mapToDouble(it -> it.overhead).summaryStatistics().getAverage(),
                iterationsResults.stream().mapToDouble(it -> it.throughput).summaryStatistics().getAverage());
    }

}
