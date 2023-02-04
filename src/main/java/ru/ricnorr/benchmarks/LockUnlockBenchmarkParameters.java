package ru.ricnorr.benchmarks;

public class LockUnlockBenchmarkParameters extends BenchmarkParameters {

    public boolean isLight;

    public LockUnlockBenchmarkParameters(int threads, LockType lockType, String lockSpec, boolean isLight, int actionsPerThread) {
        super(threads, lockType, actionsPerThread, lockSpec);
        this.isLight = isLight;
    }

    @Override
    public String getBenchmarkName() {
        return String.format(
                "Lock unlock benchmark, actions for thread = %d",
                actionsPerThread
        );
    }
}
