package ru.ricnorr.benchmarks.jmh;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.ricnorr.benchmarks.*;
import ru.ricnorr.benchmarks.jmh.cpu.JmhParConsumeCpuTokensBenchmark;
import ru.ricnorr.benchmarks.jmh.cpu.JmhConsumeCpuTokensUtil;
import ru.ricnorr.benchmarks.jmh.cpu.JmhSeqConsumeCpuTokensBenchmarkOversubscription;
import ru.ricnorr.benchmarks.jmh.cpu.JmhSeqConsumeCpuTokensBenchmarkUndersubsription;
import ru.ricnorr.benchmarks.jmh.matrix.JmhMatrixUtil;
import ru.ricnorr.benchmarks.jmh.matrix.JmhParMatrixBenchmark;
import ru.ricnorr.benchmarks.jmh.matrix.JmhSeqMatrixBenchmarkOversubscription;
import ru.ricnorr.benchmarks.jmh.matrix.JmhSeqMatrixBenchmarkUndersubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;

public class JmhBenchmarkRunner {

    public static List<BenchmarkParameters> fillBenchmarkParameters(
            List<Integer> threads,
            JSONArray locks,
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
                        for (Object lockDescription : locks) {
                            var lockName = (String)((JSONObject)lockDescription).get("name");
                            var lockSpec = (JSONObject)((JSONObject)lockDescription).get("spec");
                            var lockSpecString = "";
                            if (lockSpec == null) {
                                lockSpecString = "{}";
                            } else {
                                lockSpecString = lockSpec.toJSONString();
                            }
                            paramList.add(new MatrixBenchmarkParameters(thread, LockType.valueOf(lockName), lockSpecString, before, in, actionsCount / thread, beforeMatrixMultTimeNanos, inMatrixMultTimeNanos));
                        }
                    }
                }
                case "consumeCpu" -> {
                    long before =  ((long) obj.get("before"));
                    long in = ((long) obj.get("in"));
                    double beforeConsumeCpuTokensTimeNanos = JmhConsumeCpuTokensUtil.estimateConsumeCpuTokensTimeNanos(before);
                    double inConsumeCpuTokensTimeNanos = JmhConsumeCpuTokensUtil.estimateConsumeCpuTokensTimeNanos(in);
                    for (int thread : threads) {
                        for (Object lockDescription : locks) {
                            var lockName = (String)((JSONObject)lockDescription).get("name");
                            var lockSpec = (JSONObject)((JSONObject)lockDescription).get("spec");
                            var lockSpecString = "";
                            if (lockSpec == null) {
                                lockSpecString = "{}";
                            } else {
                                lockSpecString = lockSpec.toJSONString();
                            }
                            paramList.add(new ConsumeCpuBenchmarkParameters(thread, LockType.valueOf(lockName), lockSpecString, before, in, actionsCount / thread, beforeConsumeCpuTokensTimeNanos, inConsumeCpuTokensTimeNanos));
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

    public static double runBenchmarkNano(Class<?> clazz, int iterations, int warmupIterations, Map<String, String> params) throws RunnerException {
        var optionsBuilder = new OptionsBuilder()
                .include(clazz.getSimpleName())
                .operationsPerInvocation(1)
                .warmupIterations(warmupIterations)
                .forks(1)
                .measurementIterations(iterations)
                .verbosity(NORMAL);
        for (Map.Entry<String, String> x : params.entrySet()) {
            optionsBuilder = optionsBuilder.param(x.getKey(), x.getValue());
        }

        var res = new Runner(optionsBuilder.build()).run();
        for (BenchmarkResult x : res.stream().findFirst().get().getBenchmarkResults()) {
            assert(x.getPrimaryResult().getScoreUnit().equals("ns/op"));
            return x.getPrimaryResult().getScore();
        }
        throw new BenchmarkException("Can't get jmh benchmark result");
    }

    public static BenchmarkResultsCsv runBenchmark(int iterations, int warmupIterations, BenchmarkParameters parameters) throws RunnerException {
        if (parameters instanceof MatrixBenchmarkParameters matrixParam) {
            System.out.printf("Start benchmark: threads %d, lockType %s, lockSpec %s, beforeSize %d, inSize %d%n", parameters.threads, parameters.lockType, parameters.lockSpec, matrixParam.beforeSize, matrixParam.inSize);
            double withLocksNanos = runBenchmarkNano(JmhParMatrixBenchmark.class, iterations, warmupIterations, Map.of(
                        "beforeSize", Integer.toString(matrixParam.beforeSize),
                        "inSize", Integer.toString(matrixParam.inSize),
                        "threads", Integer.toString(matrixParam.threads),
                        "actionsPerThread", Integer.toString(matrixParam.actionsPerThread),
                        "lockType", matrixParam.lockType.toString(),
                        "lockSpec", matrixParam.lockSpec
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
            return new BenchmarkResultsCsv(parameters.getBenchmarkName(), parameters.lockType.name() + "_" + parameters.lockSpec, parameters.threads, overheadNanos, throughputNanos);
        } else if (parameters instanceof ConsumeCpuBenchmarkParameters param) {
            System.out.printf("Start benchmark: threads %d, lockType %s, lockSpec %s, beforeTokens %d, inTokens %d%n", parameters.threads, parameters.lockType, parameters.lockSpec, param.beforeCpuTokens, param.inCpuTokens);
            double withLocksNanos = runBenchmarkNano(JmhParConsumeCpuTokensBenchmark.class, iterations, warmupIterations, Map.of(
                    "beforeCpuTokens", Long.toString(param.beforeCpuTokens),
                    "inCpuTokens", Long.toString(param.inCpuTokens),
                    "threads", Integer.toString(param.threads),
                    "actionsPerThread", Integer.toString(param.actionsPerThread),
                    "lockType", param.lockType.toString(),
                    "lockSpec", param.lockSpec
            ));
            double withoutLocksNanos;
            if (param.inConsumeCpuTokensTimeNanos * param.threads > param.beforeConsumeCpuTokensTimeNanos) {
                withoutLocksNanos = runBenchmarkNano(JmhSeqConsumeCpuTokensBenchmarkOversubscription.class, iterations, warmupIterations,  Map.of(
                        "beforeCpuTokens", Long.toString(param.beforeCpuTokens),
                        "inCpuTokens", Long.toString(param.inCpuTokens),
                        "actionsPerThread", Integer.toString(param.actionsPerThread),
                        "threads", Integer.toString(param.threads))
                );
            } else {
                withoutLocksNanos = runBenchmarkNano(JmhSeqConsumeCpuTokensBenchmarkUndersubsription.class, iterations, warmupIterations, Map.of("beforeCpuTokens", Long.toString(param.beforeCpuTokens), "inCpuTokens", Long.toString(param.inCpuTokens), "actionsPerThread", Integer.toString(param.actionsPerThread)));
            }
            double overheadNanos = withLocksNanos - withoutLocksNanos;
            double throughputNanos = (parameters.threads * parameters.actionsPerThread) / withLocksNanos;
            return new BenchmarkResultsCsv(parameters.getBenchmarkName(), parameters.lockType.name() + "_" + parameters.lockSpec, parameters.threads, overheadNanos, throughputNanos);
        } else {
            throw new BenchmarkException("Cannot run jmh benchmark");
        }
    }
}
