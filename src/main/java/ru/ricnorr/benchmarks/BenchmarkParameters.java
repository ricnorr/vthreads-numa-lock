package ru.ricnorr.benchmarks;

public abstract class BenchmarkParameters {
    public int threads;
    public LockType lockType;

    public int actionsPerThread;

    public BenchmarkParameters(int threads, LockType lockType, int actionsPerThread) {
        this.threads = threads;
        this.lockType = lockType;
        this.actionsPerThread = actionsPerThread;
    }

    public abstract String getBenchmarkName();
}
