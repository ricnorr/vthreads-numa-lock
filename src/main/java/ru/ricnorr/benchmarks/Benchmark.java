package ru.ricnorr.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

public class Benchmark {

    private record IterationResult(int threads, long actionsCount, List<Long> latencies) {
    }

    private IterationResult runIteration(int threads, Runnable action, long timeoutInMillis) {
        final CyclicBarrier ready = new CyclicBarrier(threads);
        Queue<Long> latencies = new ConcurrentLinkedDeque<>();
        Queue<Long> actions = new ConcurrentLinkedDeque<>();
        List<Thread> threadList = new ArrayList<>();
        System.out.println("Run iteration");
        for (int i = 0; i < threads; i++) {
            threadList.add(new Thread(() -> {
                try {
                    ready.await();
                } catch (BrokenBarrierException | InterruptedException e) {
                    throw new BechmarkException("Failing to await threads", e);
                }
                long countActions = 0;
                long start = System.currentTimeMillis();
                List<Long> localLatencies = new ArrayList<>();
                while (System.currentTimeMillis() - start < timeoutInMillis) {
                    long latencyStart = System.currentTimeMillis();
                    action.run();
                    long latencyStop = System.currentTimeMillis();
                    countActions++;
                    localLatencies.add(latencyStop - latencyStart);
                }
                latencies.addAll(localLatencies);
                actions.add(countActions);
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
        if (actions.size() != threads) {
            throw new BechmarkException("opsCount size != threads");
        }
        System.out.println("Iteration ended");
        return new IterationResult(threads, actions.stream().mapToLong(it -> it).sum(), new ArrayList<>(latencies));
    }

    private long percentile(List<Long> latencies, double percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * latencies.size());
        return latencies.get(index-1);
    }

    public BenchmarkResult benchmark(
        int threads,
        Runnable action,
        long durationInMillis,
        int iterations,
        int warmupIterations,
        double latencyPercentile
    ) {
        System.out.println("Run warmup");
        for (int i = 0; i < warmupIterations; i++) {
            runIteration(threads, action, durationInMillis);
        }
        System.out.println("Warmup ended");

        List<IterationResult> iterationsResults = new ArrayList<>();
        System.out.println("Run trial");
        for (int i = 0; i < iterations; i++) {
            iterationsResults.add(runIteration(threads, action, durationInMillis));
        }
        System.out.println("Benchmark completed");
        System.out.println("Aggregating results");
        List<Long> allSortedLatencies =
            iterationsResults.stream().flatMap(it -> it.latencies.stream()).sorted().collect(Collectors.toList());
        long latencyPercentileElement = percentile(allSortedLatencies, latencyPercentile);
        long totalActionsCount = iterationsResults.stream().mapToLong(it -> it.actionsCount).sum();
        long totalLatencySum = allSortedLatencies.stream().mapToLong(it -> it).sum();

        return new BenchmarkResult(totalActionsCount / (totalLatencySum * 1.0), latencyPercentileElement);
    }

}
