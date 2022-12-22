package ru.ricnorr.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BenchmarkRunner {
    int iterations;

    public BenchmarkRunner(int iterations) {
        this.iterations = iterations;
    }

    private record IterationResult(int threads, double overheadNanos, double throughputNanos) {
    }

    private long measureDurationForActionNanos(int threads, int actionsPerThread, Runnable runnable) {
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
        int actionsPerThread,
        Runnable actionWithLock,
        Runnable actionWithoutLock
    ) {
        List<IterationResult> iterationsResults = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            System.out.println("Run iteration " + i);
            long withLockNanos = measureDurationForActionNanos(threads, actionsPerThread, actionWithLock);
            long withoutLockNanos = measureDurationForActionNanos(1, 1, actionWithoutLock);
            double overheadNanos = withLockNanos - withoutLockNanos;
            double throughputNanos = (actionsPerThread * threads) / (withLockNanos * 1.0);

            iterationsResults.add(new IterationResult(threads, overheadNanos, throughputNanos));
            System.out.println("End iteration " + i);
        }

        return new BenchmarkResult(iterationsResults.stream().mapToDouble(it -> it.overheadNanos).summaryStatistics().getAverage(),
                iterationsResults.stream().mapToDouble(it -> it.throughputNanos).summaryStatistics().getAverage());
    }

}
