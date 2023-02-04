package ru.ricnorr.benchmarks.params;

import ru.ricnorr.benchmarks.LockType;

public class LockUnlockBenchmarkParameters extends BenchmarkParameters {


    public LockUnlockBenchmarkParameters(int threads, LockType lockType, String lockSpec, boolean isLight, int actionsPerThread) {
        super(threads, lockType, actionsPerThread, lockSpec, isLight);
    }

    @Override
    public String getBenchmarkName() {
        return String.format(
                "Lock unlock benchmark, actions for thread = %d",
                actionsPerThread
        );
    }
}
