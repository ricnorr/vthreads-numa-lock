package ru.ricnorr.benchmarks.params;

import ru.ricnorr.benchmarks.LockType;

import java.util.Map;

public abstract class BenchmarkParameters {
    public int threads;
    public final String lockSpec;

    public final LockType lockType;

    public final int actionsPerThread;
    public final boolean isLightThread;

    public final int warmupIterations;

    public final int measurementIterations;

    public final int forks;

    public final Map<String, String> profilerParams;

    public BenchmarkParameters(
            int threads, LockType lockType, int actionsPerThread,
            String lockSpec, boolean isLightThread, int warmupIterations,
            int measurementIterations, int forks, Map<String, String> profilerParams) {
        this.threads = threads;
        this.lockType = lockType;
        this.lockSpec = lockSpec;
        this.actionsPerThread = actionsPerThread;
        this.isLightThread = isLightThread;
        this.warmupIterations = warmupIterations;
        this.measurementIterations = measurementIterations;
        this.forks = forks;
        this.profilerParams = profilerParams;
    }

    public abstract String getBenchmarkName();
}
