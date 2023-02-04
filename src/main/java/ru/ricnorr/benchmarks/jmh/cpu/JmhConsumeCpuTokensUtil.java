package ru.ricnorr.benchmarks.jmh.cpu;

import org.openjdk.jmh.runner.RunnerException;

import java.util.Map;

import static ru.ricnorr.benchmarks.jmh.JmhBenchmarkRunner.runBenchmarkNano;

public class JmhConsumeCpuTokensUtil {
    public static double estimateConsumeCpuTokensTimeNanos(long cpuTokens) throws RunnerException {
        System.out.printf("Run estimate consume %d cpu tokens time nanos%n", cpuTokens);
        return runBenchmarkNano(JmhConsumeCpuTokensBenchmark.class, 1, 0, Map.of("cpuTokens", Long.toString(cpuTokens)));
    }

    public static double estimateHighContentionWithoutLocksTimeNanos(long beforeCpu, long inCpu, long totalActions) throws RunnerException {
        System.out.printf(
                "Run estimate high contention without locks time nanos, beforeCpu=%d,inCpu=%d,totalActions=%d%n",
                beforeCpu,
                inCpu,
                totalActions
        );
        return runBenchmarkNano(JmhSeqConsumeCpuTokensBenchmarkHighContention.class, 1, 0, Map.of("beforeCpuTokens", Long.toString(beforeCpu), "inCpuTokens", Long.toString(inCpu), "totalActions", Long.toString(totalActions)));
    }
}
