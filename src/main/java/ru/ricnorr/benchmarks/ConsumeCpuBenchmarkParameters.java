package ru.ricnorr.benchmarks;

public class ConsumeCpuBenchmarkParameters extends BenchmarkParameters {

    public long beforeCpuTokens;

    public long inCpuTokens;

    public double inConsumeCpuTokensTimeNanos;

    public double beforeConsumeCpuTokensTimeNanos;

    public boolean isHigh;

    public ConsumeCpuBenchmarkParameters(int threads, LockType lockType, String lockSpec, long beforeCpuTokens, long inCpuTokens, int actionsPerThread,
                                     double beforeConsumeCpuTokensTimeNanos, double inConsumeCpuTokensTimeNanos) {
        super(threads, lockType, actionsPerThread, lockSpec);
        this.beforeCpuTokens = beforeCpuTokens;
        this.inCpuTokens = inCpuTokens;
        this.beforeConsumeCpuTokensTimeNanos = beforeConsumeCpuTokensTimeNanos;
        this.inConsumeCpuTokensTimeNanos = inConsumeCpuTokensTimeNanos;
        this.isHigh = inConsumeCpuTokensTimeNanos * threads > beforeConsumeCpuTokensTimeNanos;
    }

    @Override
    public String getBenchmarkName() {
        return String.format(
                "Consume cpu tokens. Before crit.section: %d tokens. In crit.section: %d tokens (%s)",
                beforeCpuTokens,
                inCpuTokens,
                isHigh ? "High contention" : "Low contention"
        );
    }

}