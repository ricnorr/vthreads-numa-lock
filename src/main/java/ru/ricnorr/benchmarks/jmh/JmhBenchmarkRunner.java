package ru.ricnorr.benchmarks.jmh;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openjdk.jmh.runner.RunnerException;
import ru.ricnorr.benchmarks.*;
import ru.ricnorr.benchmarks.jmh.matrix.JmhMatrixUtil;
import ru.ricnorr.benchmarks.jmh.matrix.JmhParMatrixBenchmark;
import ru.ricnorr.benchmarks.jmh.matrix.JmhSeqMatrixBenchmarkOversubscription;
import ru.ricnorr.benchmarks.jmh.matrix.JmhSeqMatrixBenchmarkUndersubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.ricnorr.benchmarks.jmh.matrix.JmhMatrixUtil.runBenchmarkNano;

public class JmhBenchmarkRunner {

    public static List<BenchmarkParameters> fillBenchmarkParameters(
            List<Integer> threads,
            List<LockType> lockTypes,
            JSONArray array,
            int actionsCount
    ) throws RunnerException {
        List<BenchmarkParameters> paramList = new ArrayList<>();
        for (Object o : array) {
            JSONObject obj = (JSONObject) o;
            String name = (String) obj.get("name");
            switch (name) {
                case "matrix" -> {
                    int before = (int) ((long) obj.get("before"));
                    int in = (int) ((long) obj.get("in"));
                    double beforeMatrixMultTimeNanos = JmhMatrixUtil.estimateMatrixMultiplicationTimeNanos(before);
                    double inMatrixMultTimeNanos = JmhMatrixUtil.estimateMatrixMultiplicationTimeNanos(in);
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

    public static BenchmarkResultsCsv runBenchmark(int iterations, int warmupIterations, BenchmarkParameters parameters) throws RunnerException {
        if (parameters instanceof MatrixBenchmarkParameters matrixParam) {
            System.out.printf("Start benchmark: threads %d, lockType %s, beforeSize %d, inSize %d%n", parameters.threads, parameters.lockType, matrixParam.beforeSize, matrixParam.inSize);
            double withLocksNanos = runBenchmarkNano(JmhParMatrixBenchmark.class, iterations, warmupIterations, Map.of(
                        "beforeSize", Integer.toString(matrixParam.beforeSize),
                        "inSize", Integer.toString(matrixParam.inSize),
                        "threads", Integer.toString(matrixParam.threads),
                        "actionsPerThread", Integer.toString(matrixParam.actionsPerThread),
                        "lockType", matrixParam.lockType.toString()
                    ));
            double withoutLocksNanos;
            if (matrixParam.inMatrixMultTimeNanos * matrixParam.threads > matrixParam.beforeMatrixMultTimeNanos) {
                withoutLocksNanos = runBenchmarkNano(JmhSeqMatrixBenchmarkOversubscription.class, iterations, warmupIterations,  Map.of(
                        "beforeSize", Integer.toString(matrixParam.beforeSize),
                        "inSize", Integer.toString(matrixParam.inSize),
                        "actionsPerThread", Integer.toString(matrixParam.actionsPerThread),
                        "threads", Integer.toString(matrixParam.threads))
                );
            } else {
                withoutLocksNanos = runBenchmarkNano(JmhSeqMatrixBenchmarkUndersubscription.class, iterations, warmupIterations, Map.of("beforeSize", Integer.toString(matrixParam.beforeSize), "inSize", Integer.toString(matrixParam.inSize), "actionsPerThread", Integer.toString(matrixParam.actionsPerThread)));
            }
            double overheadNanos = withLocksNanos - withoutLocksNanos;
            double throughputNanos = (parameters.threads * parameters.actionsPerThread) / withLocksNanos;
            return new BenchmarkResultsCsv(parameters.getBenchmarkName(), parameters.lockType.toString(), parameters.threads, overheadNanos, throughputNanos);
        } else {
            throw new BenchmarkException("Cannot run jmh benchmark");
        }
    }
}
