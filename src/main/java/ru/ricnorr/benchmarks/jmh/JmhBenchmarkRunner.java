package ru.ricnorr.benchmarks.jmh;

import kotlin.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.ricnorr.benchmarks.BenchmarkException;
import ru.ricnorr.benchmarks.BenchmarkResultsCsv;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.benchmarks.jmh.cpu.JmhConsumeCpuTokensUtil;
import ru.ricnorr.benchmarks.jmh.cpu.JmhParConsumeCpuTokensBenchmark;
import ru.ricnorr.benchmarks.params.BenchmarkParameters;
import ru.ricnorr.benchmarks.params.ConsumeCpuBenchmarkParameters;
import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;
import static ru.ricnorr.benchmarks.Main.autoThreadsInit;

public class JmhBenchmarkRunner {

    private static boolean shouldSkip(JSONObject lockDesc) {
        return (lockDesc.get("skip") != null && (Boolean) (lockDesc.get("skip")));
    }

    private static Pair<String, String> parseLockNameAndSpec(JSONObject lockDescriptionJson) {
        var lockName = (String) lockDescriptionJson.get("name");
        var lockSpec = (JSONObject) lockDescriptionJson.get("spec");
        var lockSpecString = "";
        if (lockSpec == null) {
            lockSpecString = "{}";
        } else {
            lockSpecString = lockSpec.toJSONString();
        }
        return new Pair<>(lockName, lockSpecString);
    }

    public static List<BenchmarkParameters> fillBenchmarkParameters(JSONArray locks, JSONArray array, int actionsCount) throws RunnerException {
        List<BenchmarkParameters> paramList = new ArrayList<>();
        for (Object o : array) {
            JSONObject obj = (JSONObject) o;
            if (shouldSkip(obj)) {
                continue;
            }
            String name = (String) obj.get("name");
            JSONArray threadsArray = (JSONArray) obj.get("threads");
            List<Integer> threads = new ArrayList<>();
            if (threadsArray != null) {
                for (Object value : threadsArray) {
                    threads.add((int) ((long) value));
                }
            } else {
                threads = autoThreadsInit();
            }
            int warmupIterations = (int) ((long) obj.get("warmupIterations"));
            int measurementIterations = (int) ((long) obj.get("measurementIterations"));
            int forks = (int) ((long) obj.get("forks"));
            if (name.equals("consumeCpu")) {
                long before = ((long) obj.get("before"));
                long in = ((long) obj.get("in"));
                boolean isLightThread = obj.get("light") != null && ((Boolean) obj.get("light"));
                double beforeConsumeCpuTokensTimeNanos = JmhConsumeCpuTokensUtil.estimateConsumeCpuTokensTimeNanos(before);
                double inConsumeCpuTokensTimeNanos = JmhConsumeCpuTokensUtil.estimateConsumeCpuTokensTimeNanos(in);
                double highContentionWithoutLocksNanos = JmhConsumeCpuTokensUtil.estimateHighContentionWithoutLocksTimeNanos(before, in, actionsCount);
                for (int thread : threads) {
                    for (Object lockDescription : locks) {
                        if (!shouldSkip((JSONObject) lockDescription)) {
                            var lockNameAndSpec = parseLockNameAndSpec((JSONObject) lockDescription);
                            paramList.add(
                                    new ConsumeCpuBenchmarkParameters(
                                            thread, LockType.valueOf(lockNameAndSpec.component1()),
                                            lockNameAndSpec.component2(), isLightThread,
                                            before, in, actionsCount / thread,
                                            beforeConsumeCpuTokensTimeNanos, inConsumeCpuTokensTimeNanos,
                                            highContentionWithoutLocksNanos, warmupIterations, measurementIterations,
                                            forks
                                    )
                            );
                        }
                    }
                }
            } else {
                throw new IllegalStateException("Unknown benchmark type " + name);
            }

        }
        return paramList;
    }

    public static List<Double> runBenchmarkNano(ChainedOptionsBuilder optionsBuilder, Map<String, String> params) throws RunnerException {
        for (Map.Entry<String, String> x : params.entrySet()) {
            optionsBuilder = optionsBuilder.param(x.getKey(), x.getValue());
        }

        var res = new Runner(optionsBuilder.build()).run();
        var executionTimesInNanos = new ArrayList<Double>();
        res.forEach(it -> it.getBenchmarkResults().forEach(it2 -> it2.getIterationResults().forEach(it3 -> executionTimesInNanos.add(it3.getPrimaryResult().getScore()))));
        return executionTimesInNanos;
    }

    public static BenchmarkResultsCsv runBenchmark(BenchmarkParameters parameters) throws RunnerException {
        switch (parameters) {
            case ConsumeCpuBenchmarkParameters param -> {
                System.out.println(param.logBegin());
                var options = new OptionsBuilder().include(JmhParConsumeCpuTokensBenchmark.class.getSimpleName())
                        .warmupIterations(param.warmupIterations)
                        .forks(2)
                        .measurementIterations(param.measurementIterations)
                        .forks(param.forks)
                        .verbosity(NORMAL);
                List<Double> withLocksNanos = runBenchmarkNano(options, Map.of("isLightThread", Boolean.toString(param.isLightThread), "beforeCpuTokens", Long.toString(param.beforeCpuTokens), "inCpuTokens", Long.toString(param.inCpuTokens), "threads", Integer.toString(param.threads), "actionsPerThread", Integer.toString(param.actionsPerThread), "lockType", param.lockType.toString(), "lockSpec", param.lockSpec));
                double withLockNanosMin = withLocksNanos.stream().min(Double::compare).get();
                double withLockNanosMax = withLocksNanos.stream().max(Double::compare).get();
                double withLockNanosMedian = Utils.median(withLocksNanos);
                double withoutLocksNanos;
                if (param.isHighContention) {
                    withoutLocksNanos = param.beforeConsumeCpuTokensTimeNanos;
                } else {
                    throw new RunnerException("No low contention supported");
                    //withoutLocksNanos = runBenchmarkNano(JmhSeqConsumeCpuTokensBenchmarkLowContention.class, iterations, warmupIterations, Map.of("beforeCpuTokens", Long.toString(param.beforeCpuTokens), "inCpuTokens", Long.toString(param.inCpuTokens), "actionsPerThread", Integer.toString(param.actionsPerThread))).stream().findFirst().get();
                }
                double overheadNanosMin = withLockNanosMin - withoutLocksNanos;
                double overheadNanosMax = withLockNanosMax - withoutLocksNanos;
                double overheadNanosAverage = withLockNanosMedian - withoutLocksNanos;
                double throughputNanosMin = (parameters.threads * parameters.actionsPerThread) / withLockNanosMax;
                double throughputNanosMax = (parameters.threads * parameters.actionsPerThread) / withLockNanosMin;
                double throughputNanosMedian = (parameters.threads * parameters.actionsPerThread) / withLockNanosMedian;
                System.out.printf("Consume cpu bench: i got max_over=%f, min_over=%f, avg_over=%f, max_thrpt=%f, min_thrpt=%f, avg_thrpt=%f%n", overheadNanosMax, overheadNanosMin, overheadNanosAverage, throughputNanosMax, throughputNanosMin, throughputNanosMedian);
                return new BenchmarkResultsCsv(parameters.getBenchmarkName(), parameters.lockType.name(), parameters.threads, overheadNanosMax, overheadNanosMin, overheadNanosAverage, throughputNanosMax, throughputNanosMin, throughputNanosMedian);
            }
            case null, default -> throw new BenchmarkException("Cannot run jmh benchmark");
        }
    }
}
