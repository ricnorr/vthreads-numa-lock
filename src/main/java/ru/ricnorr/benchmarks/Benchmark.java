package ru.ricnorr.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CyclicBarrier;

public class Benchmark {

    private record IterationResult(int threads, long actionsCount, long latencySum) {
    }

    private BenchmarkResult runIteration(int threads, Runnable action, long timeoutInMillis) {
        final CyclicBarrier ready = new CyclicBarrier(threads);
        Queue<IterationResult> results = new ConcurrentLinkedDeque<>();
        List<Thread> threadList = new ArrayList<>();
        System.out.println("Run iteration");
        for (int i = 0; i < threads; i++) {
            threadList.add(new Thread(() -> {
                try {
                    ready.await();
                } catch (BrokenBarrierException | InterruptedException e) {
                    throw new BechmarkException("Failing to await threads", e);
                }
                long latencySum = 0;
                long countActions = 0;
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < timeoutInMillis) {
                    long latencyStart = System.currentTimeMillis();
                    action.run();
                    long latencyStop = System.currentTimeMillis();
                    countActions++;
                    latencySum += latencyStop - latencyStart;
                }
                results.add(new IterationResult(threads, countActions, latencySum));
            }));
        }
        for (Thread thread : threadList) {
            thread.start();
        }
        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new BechmarkException("Can't join threads", e);
            }
        }
        if (results.size() != threads) {
            throw new BechmarkException("opsCount size != threads");
        }
        System.out.println("Iteration ended");
        long totalActionsCount = results.stream().mapToLong(it -> it.actionsCount).sum();
        long totalLatency = results.stream().mapToLong(it -> it.latencySum).sum();
        double throughput = totalActionsCount / (timeoutInMillis * 1.0);
        double latency = totalLatency / (totalActionsCount * 1.0);
        return new BenchmarkResult(throughput, latency);
    }

    private BenchmarkResult avgBenchmarkResult(List<BenchmarkResult> iterationsResult) {
        return new BenchmarkResult(
            iterationsResult.stream().mapToDouble(BenchmarkResult::throughput).sum() / iterationsResult.size(),
            iterationsResult.stream().mapToDouble(BenchmarkResult::latency).sum() / iterationsResult.size()
        );
    }

    public BenchmarkResult benchmark(
        int threads,
        Runnable action,
        long durationInMillis,
        int iterations,
        int warmupIterations
    ) {
        System.out.println("Run warmup");
        for (int i = 0; i < warmupIterations; i++) {
            runIteration(threads, action, durationInMillis);
        }
        System.out.println("Warmup ended");

        List<BenchmarkResult> iterationsResults = new ArrayList<>();
        System.out.println("Run trial");
        for (int i = 0; i < iterations; i++) {
            iterationsResults.add(runIteration(threads, action, durationInMillis));
        }
        System.out.println("Benchmark completed");

        return avgBenchmarkResult(iterationsResults);
    }

}
