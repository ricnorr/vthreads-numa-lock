package ru.ricnorr.benchmarks.custom;

import org.ejml.concurrency.EjmlConcurrency;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.ricnorr.benchmarks.*;
import ru.ricnorr.benchmarks.custom.matrix.CustomMatrixUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;

import static ru.ricnorr.benchmarks.Main.*;

public class CustomBenchmarkRunner {
    public static List<BenchmarkParameters> fillBenchmarkParameters(
            List<Integer> threads,
            List<LockType> lockTypes,
            JSONArray array,
            int actionsCount
    ) {
        List<BenchmarkParameters> paramList = new ArrayList<>();
        for (Object o : array) {
            JSONObject obj = (JSONObject) o;
            String name = (String) obj.get("name");
            switch (name) {
                case "matrix" -> {
                    int before = (int) ((long) obj.get("before"));
                    int in = (int) ((long) obj.get("in"));
                    double beforeMatrixMultTimeNanos = CustomMatrixUtil.estimateMatrixMultiplicationTimeNanos(before);
                    double inMatrixMultTimeNanos = CustomMatrixUtil.estimateMatrixMultiplicationTimeNanos(in);
                    for (int thread : threads) {
                        for (LockType lockType : lockTypes) {
                            paramList.add(new MatrixBenchmarkParameters(thread, lockType, before, in, actionsCount / thread, beforeMatrixMultTimeNanos, inMatrixMultTimeNanos));
                        }
                    }

                }
                default -> {
                    throw new IllegalStateException("Unknown benchmark type " + name);
                }
            }

        }
        return paramList;
    }

    private static long measureDurationForActionNanos(int threads, int actionsPerThread, Runnable runnable) {
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

    public static BenchmarkResultsCsv runBenchmark(int iterations, BenchmarkParameters param) {
        Lock lock = initLock(param.lockType);
        Runnable withLockRunnable;
        Runnable withoutLockRunnable;
        if (param instanceof MatrixBenchmarkParameters matrixParam) {
            withLockRunnable = CustomMatrixUtil.initMatrixWithLockRunnable(lock, matrixParam);
            withoutLockRunnable = CustomMatrixUtil.initMatrixWithoutLockRunnable(matrixParam);
        } else {
            throw new BenchmarkException("Cannot init runnable for parameter");
        }
        System.out
                .printf(
                        "Run bench,name=%s,threads=%d,lock=%s%n",
                        param.getBenchmarkName(),
                        param.threads,
                        param.lockType.name()
                );

        EjmlConcurrency.USE_CONCURRENT = false;
        setAffinity(param.threads, ProcessHandle.current().pid(), getProcessorsNumbersInNumaNodeOrder());

        double totalOverheadNanos = 0;
        double totalThroughputNanos = 0;
        for (int i = 0; i < iterations; i++) {
            System.out.println("Run iteration " + i);
            long withLockNanos = measureDurationForActionNanos(param.threads, param.actionsPerThread, withLockRunnable);
            long withoutLockNanos = measureDurationForActionNanos(param.threads, param.actionsPerThread, withoutLockRunnable);
            double overheadNanos = withLockNanos - withoutLockNanos;
            double throughputNanos = (param.actionsPerThread * param.threads) / (withLockNanos * 1.0);

            totalOverheadNanos += overheadNanos;
            totalThroughputNanos += throughputNanos;
            System.out.println("End iteration " + i);
        }

        System.out.println("Bench ended");
        return new BenchmarkResultsCsv(
                param.getBenchmarkName(),
                param.lockType.name(),
                param.threads,
                totalOverheadNanos / iterations,
                totalThroughputNanos / iterations
        );
    }
}
