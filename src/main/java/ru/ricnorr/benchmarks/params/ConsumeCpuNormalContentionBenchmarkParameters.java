package ru.ricnorr.benchmarks.params;

import ru.ricnorr.benchmarks.LockType;

public class ConsumeCpuNormalContentionBenchmarkParameters extends BenchmarkParameters {

    public long beforeCpuTokens;

    public ConsumeCpuNormalContentionBenchmarkParameters(int threads, LockType lockType, String lockSpec, boolean isLightThread, long beforeCpuTokens, int actionsPerThread) {
        super(threads, lockType, actionsPerThread, lockSpec, isLightThread);
        this.beforeCpuTokens = beforeCpuTokens;
    }

    @Override
    public String getBenchmarkName() {
        return String.format(
                "Consume cpu tokens. Before cpuTokens: %d (Normal contention)",
                beforeCpuTokens
        );
    }

}
