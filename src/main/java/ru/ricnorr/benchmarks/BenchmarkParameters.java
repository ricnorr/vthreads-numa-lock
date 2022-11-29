package ru.ricnorr.benchmarks;

abstract class BenchmarkParameters {
    int threads;
    LockType lockType;

    public BenchmarkParameters(int threads, LockType lockType) {
        this.threads = threads;
        this.lockType = lockType;
    }

    abstract String getBenchmarkName();
}
