package ru.ricnorr.benchmarks;

public class ConsumeCpuNormalContentionBenchmarkParameters extends BenchmarkParameters {

    public long beforeCpuTokens;

    public ConsumeCpuNormalContentionBenchmarkParameters(int threads, LockType lockType, String lockSpec, long beforeCpuTokens, int actionsPerThread) {
        super(threads, lockType, actionsPerThread, lockSpec);
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