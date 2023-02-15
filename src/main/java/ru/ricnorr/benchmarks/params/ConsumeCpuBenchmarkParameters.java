package ru.ricnorr.benchmarks.params;

import ru.ricnorr.benchmarks.LockType;

public class ConsumeCpuBenchmarkParameters extends BenchmarkParameters {

    public long beforeCpuTokens;

    public long inCpuTokens;

    public double inConsumeCpuTokensTimeNanos;

    public double beforeConsumeCpuTokensTimeNanos;

    public boolean isHighContention;


    public double highContentionWithoutLockNanos;

    public ConsumeCpuBenchmarkParameters(int threads, LockType lockType, String lockSpec, boolean isLightThread, long beforeCpuTokens, long inCpuTokens, int actionsPerThread,
                                         double beforeConsumeCpuTokensTimeNanos, double inConsumeCpuTokensTimeNanos, double highContentionWithoutLockNanos) {
        super(threads, lockType, actionsPerThread, lockSpec, isLightThread);
        this.beforeCpuTokens = beforeCpuTokens;
        this.inCpuTokens = inCpuTokens;
        this.beforeConsumeCpuTokensTimeNanos = beforeConsumeCpuTokensTimeNanos;
        this.inConsumeCpuTokensTimeNanos = inConsumeCpuTokensTimeNanos;
        this.isHighContention = inConsumeCpuTokensTimeNanos * threads > beforeConsumeCpuTokensTimeNanos;
        this.highContentionWithoutLockNanos = highContentionWithoutLockNanos;
    }

    @Override
    public String getBenchmarkName() {
        return String.format(
                "Consume cpu tokens. %s threads. Before crit.section: %d tokens. In crit.section: %d tokens (%s)",
                isLightThread ? "Light" : "Hard",
                beforeCpuTokens,
                inCpuTokens,
                isHighContention ? "High contention" : "Low contention"
        );
    }

    public String logBegin() {
        return String.format(
                "%nStart %s contention consume cpu benchmark: threads=%d, lightThreads=%b, lockType=%s, lockSpec=%s, beforeTokens=%d, inTokens=%d, beforeTimeNanos=%f, inTimeNanos=%f%n",
                isHighContention ? "high" : "low",
                threads,
                isLightThread,
                lockType,
                lockSpec,
                beforeCpuTokens,
                inCpuTokens,
                beforeConsumeCpuTokensTimeNanos,
                inConsumeCpuTokensTimeNanos
        );
    }

}
