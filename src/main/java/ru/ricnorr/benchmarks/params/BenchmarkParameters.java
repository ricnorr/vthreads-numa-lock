package ru.ricnorr.benchmarks.params;

import ru.ricnorr.benchmarks.LockType;

public abstract class BenchmarkParameters {
    public int threads;
    public String lockSpec;

    public LockType lockType;

    public int actionsPerThread;
    public boolean isLightThread;

    public BenchmarkParameters(int threads, LockType lockType, int actionsPerThread, String lockSpec, boolean isLightThread) {
        this.threads = threads;
        this.lockType = lockType;
        this.lockSpec = lockSpec;
        this.actionsPerThread = actionsPerThread;
        this.isLightThread = isLightThread;
    }

    public abstract String getBenchmarkName();
}
