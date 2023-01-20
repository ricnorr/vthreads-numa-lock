package ru.ricnorr.benchmarks.jmh.cpu;

import org.openjdk.jmh.runner.RunnerException;

import java.util.Map;

import static ru.ricnorr.benchmarks.jmh.JmhBenchmarkRunner.runBenchmarkNano;

public class JmhConsumeCpuTokensUtil {
    public static double estimateConsumeCpuTokensTimeNanos(long cpuTokens) throws RunnerException {
        return runBenchmarkNano(JmhConsumeCpuTokensBenchmark.class, 1, 0, Map.of("cpuTokens", Long.toString(cpuTokens)));
    }
}
