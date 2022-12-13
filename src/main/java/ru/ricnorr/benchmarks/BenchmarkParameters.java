package ru.ricnorr.benchmarks;

abstract class BenchmarkParameters {
    int threads;
    LockType lockType;

    int actionsCount;

    public BenchmarkParameters(int threads, LockType lockType, int actionsCount) {
        this.threads = threads;
        this.lockType = lockType;
        this.actionsCount = actionsCount;
    }

    abstract String getBenchmarkName();
}
