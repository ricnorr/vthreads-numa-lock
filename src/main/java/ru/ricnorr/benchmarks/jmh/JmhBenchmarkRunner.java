package ru.ricnorr.benchmarks.jmh;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import ru.ricnorr.benchmarks.LockUnlockBenchmarkParameters;
import ru.ricnorr.benchmarks.*;
import ru.ricnorr.benchmarks.jmh.cpu.JmhConsumeCpuTokensUtil;
import ru.ricnorr.benchmarks.jmh.cpu.JmhParConsumeCpuTokensBenchmark;
import ru.ricnorr.benchmarks.jmh.cpu.JmhSeqConsumeCpuTokensBenchmarkHighContention;
import ru.ricnorr.benchmarks.jmh.cpu.JmhSeqConsumeCpuTokensBenchmarkLowContention;
import ru.ricnorr.benchmarks.jmh.lockunlock.JmhLockUnlockBenchmark;
import ru.ricnorr.benchmarks.jmh.matrix.JmhMatrixUtil;
import ru.ricnorr.benchmarks.jmh.matrix.JmhParMatrixBenchmark;
import ru.ricnorr.benchmarks.jmh.matrix.JmhSeqMatrixBenchmarkOversubscription;
import ru.ricnorr.benchmarks.jmh.matrix.JmhSeqMatrixBenchmarkUndersubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.openjdk.jmh.runner.options.VerboseMode.SILENT;

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
                            var lockName = (String) ((JSONObject) lockDescription).get("name");
                            var lockSpec = (JSONObject) ((JSONObject) lockDescription).get("spec");
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
                    long before = ((long) obj.get("before"));
                    long in = ((long) obj.get("in"));
                    var isLightThread = (Boolean) obj.get("light");
                    double beforeConsumeCpuTokensTimeNanos = JmhConsumeCpuTokensUtil.estimateConsumeCpuTokensTimeNanos(before);
                    double inConsumeCpuTokensTimeNanos = JmhConsumeCpuTokensUtil.estimateConsumeCpuTokensTimeNanos(in);
                    double highContentionWithoutLocksNanos = JmhConsumeCpuTokensUtil.estimateHighContentionWithoutLocksTimeNanos(before, in, actionsCount);
                    for (int thread : threads) {
                        for (Object lockDescription : locks) {
                            var lockDescriptionJson = (JSONObject) lockDescription;
                            var lockName = (String) lockDescriptionJson.get("name");
                            var lockSpec = (JSONObject) lockDescriptionJson.get("spec");
                            var lockSpecString = "";
                            if (lockSpec == null) {
                                lockSpecString = "{}";
                            } else {
                                lockSpecString = lockSpec.toJSONString();
                            }
                            paramList.add(new ConsumeCpuBenchmarkParameters(thread, LockType.valueOf(lockName), lockSpecString, isLightThread, before, in, actionsCount / thread, beforeConsumeCpuTokensTimeNanos, inConsumeCpuTokensTimeNanos, highContentionWithoutLocksNanos));
                        }
                    }
                }
                case "consumeCpuNormalContention" -> {
                    long before = ((long) obj.get("before"));
                    var isLightThread = (Boolean) obj.get("light");
                    for (int thread : threads) {
                        for (Object lockDescription : locks) {
                            var lockDescriptionJson = (JSONObject) lockDescription;
                            var lockName = (String) lockDescriptionJson.get("name");
                            var lockSpec = (JSONObject) lockDescriptionJson.get("spec");
                            var lockSpecString = "";
                            if (lockSpec == null) {
                                lockSpecString = "{}";
                            } else {
                                lockSpecString = lockSpec.toJSONString();
                            }
                            paramList.add(new ConsumeCpuNormalContentionBenchmarkParameters(thread, LockType.valueOf(lockName), lockSpecString, isLightThread, before, actionsCount / thread));
                        }
                    }
                }
                case "lockUnlock" -> {
                    long phases = ((long) obj.get("phases"));
                    var isLightThread = (Boolean) obj.get("light");
                    for (int thread : threads) {
                        for (Object lockDescription : locks) {
                            var lockDescriptionJson = (JSONObject) lockDescription;
                            var lockName = (String) lockDescriptionJson.get("name");
                            var lockSpec = (JSONObject) lockDescriptionJson.get("spec");
                            var lockSpecString = "";
                            if (lockSpec == null) {
                                lockSpecString = "{}";
                            } else {
                                lockSpecString = lockSpec.toJSONString();
                            }
                            paramList.add(new LockUnlockBenchmarkParameters(thread, LockType.valueOf(lockName), lockSpecString, isLightThread, (int) phases));
                        }
                    }
                }
                default -> throw new IllegalStateException("Unknown benchmark type " + name);
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
                .measurementTime(TimeValue.seconds(5))
                .measurementIterations(iterations)
                .verbosity(SILENT);
        for (Map.Entry<String, String> x : params.entrySet()) {
            optionsBuilder = optionsBuilder.param(x.getKey(), x.getValue());
        }

        var res = new Runner(optionsBuilder.build()).run();
        for (BenchmarkResult x : res.stream().findFirst().get().getBenchmarkResults()) {
            assert (x.getPrimaryResult().getScoreUnit().equals("ns/op"));
            return x.getPrimaryResult().getScore();
        }
        throw new BenchmarkException("Can't get jmh benchmark result");
    }

    public static BenchmarkResultsCsv runBenchmark(int iterations, int warmupIterations, BenchmarkParameters parameters) throws RunnerException {
        switch (parameters) {
            case MatrixBenchmarkParameters matrixParam -> {
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
                    withoutLocksNanos = runBenchmarkNano(JmhSeqMatrixBenchmarkOversubscription.class, iterations, warmupIterations, Map.of(
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
            }
            case ConsumeCpuBenchmarkParameters param -> {
                System.out.println(param.logBegin());
                double withLocksNanos = runBenchmarkNano(JmhParConsumeCpuTokensBenchmark.class, iterations, warmupIterations, Map.of(
                        "useLightThreads", Boolean.toString(param.isLightThread),
                        "beforeCpuTokens", Long.toString(param.beforeCpuTokens),
                        "inCpuTokens", Long.toString(param.inCpuTokens),
                        "threads", Integer.toString(param.threads),
                        "actionsPerThread", Integer.toString(param.actionsPerThread),
                        "lockType", param.lockType.toString(),
                        "lockSpec", param.lockSpec
                ));
                double withoutLocksNanos;
                if (param.isHighContention) {
                    withoutLocksNanos = param.beforeConsumeCpuTokensTimeNanos;
                } else {
                    withoutLocksNanos = runBenchmarkNano(JmhSeqConsumeCpuTokensBenchmarkLowContention.class, iterations, warmupIterations, Map.of("beforeCpuTokens", Long.toString(param.beforeCpuTokens), "inCpuTokens", Long.toString(param.inCpuTokens), "actionsPerThread", Integer.toString(param.actionsPerThread)));
                }
                double overheadNanos = withLocksNanos - withoutLocksNanos;
                double throughputNanos = (parameters.threads * parameters.actionsPerThread) / withLocksNanos;
                System.out.printf(
                        "Consume cpu bench: i got overhead_nanos=%f, throughput_nanos=%f%n",
                        overheadNanos,
                        throughputNanos
                );
                return new BenchmarkResultsCsv(parameters.getBenchmarkName(), parameters.lockType.name() + "_" + parameters.lockSpec, parameters.threads, overheadNanos, throughputNanos);
            }
            case ConsumeCpuNormalContentionBenchmarkParameters param -> {
                System.out.printf(
                        "Start benchmark: threads %d, lockType %s, lockSpec %s, beforeTokens %d%n",
                        parameters.threads,
                        parameters.lockType,
                        parameters.lockSpec,
                        param.beforeCpuTokens
                );
                double withLocksNanos = runBenchmarkNano(JmhParConsumeCpuTokensBenchmark.class, iterations, warmupIterations, Map.of(
                        "useLightThreads", Boolean.toString(param.isLightThread),
                        "beforeCpuTokens", Long.toString(param.beforeCpuTokens),
                        "inCpuTokens", Long.toString(param.beforeCpuTokens / param.threads),
                        "threads", Integer.toString(param.threads),
                        "actionsPerThread", Integer.toString(param.actionsPerThread),
                        "lockType", param.lockType.toString(),
                        "lockSpec", param.lockSpec
                ));
                double withoutLocksNanos = runBenchmarkNano(JmhSeqConsumeCpuTokensBenchmarkHighContention.class, iterations, warmupIterations, Map.of(
                        "beforeCpuTokens", Long.toString(param.beforeCpuTokens),
                        "inCpuTokens", Long.toString(param.beforeCpuTokens / param.threads),
                        "actionsPerThread", Integer.toString(param.actionsPerThread),
                        "threads", Integer.toString(param.threads))
                );
                System.out.printf(
                        "Average execution time of benchmark %f",
                        withoutLocksNanos
                );
                double overheadNanos = withLocksNanos - withoutLocksNanos;
                double throughputNanos = (parameters.threads * parameters.actionsPerThread) / withLocksNanos;
                return new BenchmarkResultsCsv(parameters.getBenchmarkName(), parameters.lockType.name() + "_" + parameters.lockSpec, parameters.threads, overheadNanos, throughputNanos);
            }
            case LockUnlockBenchmarkParameters param -> {
                System.out.printf(
                        "%nStart lock-unlock benchmark: threads %d, lockType %s, lockSpec %s, phases in thread %d%n",
                        parameters.threads,
                        parameters.lockType,
                        parameters.lockSpec,
                        param.actionsPerThread
                );
                double overheadNanos = runBenchmarkNano(JmhLockUnlockBenchmark.class, iterations, warmupIterations, Map.of(
                        "useLightThreads", Boolean.toString(param.isLight),
                        "threads", Integer.toString(param.threads),
                        "actionsPerThread", Integer.toString(param.actionsPerThread),
                        "lockType", param.lockType.toString(),
                        "lockSpec", param.lockSpec
                ));
                double throughputNanos = (parameters.threads * parameters.actionsPerThread) / overheadNanos;
                System.out.printf("%nEnd lock-unlock benchmark: i got overhead_nanos=%f, throughput_nanos=%f%n", overheadNanos, throughputNanos);
                return new BenchmarkResultsCsv(
                        parameters.getBenchmarkName(),
                        parameters.lockType.name() + "_" + parameters.lockSpec,
                        parameters.threads,
                        overheadNanos,
                        throughputNanos
                );
            }
            case null, default -> throw new BenchmarkException("Cannot run jmh benchmark");
        }
    }
}
