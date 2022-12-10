package ru.ricnorr.benchmarks;

import net.openhft.affinity.AffinityThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static net.openhft.affinity.AffinityStrategies.*;

public class BenchmarkRunner {

    int durationInMillis;
    int warmupIterations;
    int iterations;
    double latencyPercentile;

    public BenchmarkRunner(int durationInMillis, int warmupIterations, int iterations, double latencyPercentile) {
        this.durationInMillis = durationInMillis;
        this.warmupIterations = warmupIterations;
        this.iterations = iterations;
        this.latencyPercentile = latencyPercentile;
    }

    private record IterationResult(int threads, long actionsCount, List<Long> latencies) {
    }

    static class MyCallable implements Callable<Object> {

        List<Long> localLatencies = new ArrayList<>();
        long countActions = 0;

        Runnable runnable;

        CyclicBarrier ready;
        public MyCallable(CyclicBarrier ready, Runnable runnable) {
            this.ready = ready;
            this.runnable = runnable;
        }

        @Override
        public Object call() {
            try {
                ready.await();
            } catch (BrokenBarrierException | InterruptedException e) {
                throw new BenchmarkException("Failing to await threads", e);
            }
            while (!Thread.interrupted()) {
                long latencyStart = System.currentTimeMillis();
                runnable.run();
                long latencyStop = System.currentTimeMillis();
                countActions++;
                localLatencies.add(latencyStop - latencyStart);
            }
            return null;
        }
    }

    private IterationResult runIteration(int threads, Runnable action, long timeoutInMillis) {
        final CyclicBarrier ready = new CyclicBarrier(threads);
        List<Long> latencies = new ArrayList<>();
        List<Long> actions = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(threads, new AffinityThreadFactory("bg", DIFFERENT_CORE));
        List<Future<Object>> res;
        List<MyCallable> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(new MyCallable(ready, action));
        }
        try {
            res = executorService.invokeAll(tasks, timeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (Future<Object> fut : res) {
            while (!fut.isDone()) {
            }
        }
        for (MyCallable task : tasks) {
            latencies.addAll(task.localLatencies);
            actions.add(task.countActions);
        }

        if (actions.size() != threads) {
            throw new BenchmarkException("opsCount size != threads");
        }
        return new IterationResult(threads, actions.stream().mapToLong(it -> it).sum(), new ArrayList<>(latencies));
    }

    private long percentile(List<Long> latencies, double percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * latencies.size());
        return latencies.get(index-1);
    }

    public BenchmarkResult benchmark(
        int threads,
        Runnable action
    ) {
        List<IterationResult> iterationsResults = new ArrayList<>();
        System.out.println("Run iterations");
        for (int i = 0; i < iterations; i++) {
            System.out.println("Run iteration " + i);
            iterationsResults.add(runIteration(threads, action, durationInMillis));
            System.out.println("End iteration " + i);
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
