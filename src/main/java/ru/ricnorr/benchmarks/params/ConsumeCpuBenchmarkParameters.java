package ru.ricnorr.benchmarks.params;

import ru.ricnorr.benchmarks.LockType;

import java.util.Map;

public class ConsumeCpuBenchmarkParameters extends BenchmarkParameters {

    public long beforeCpuTokens;

    public long inCpuTokens;

    public double inConsumeCpuTokensTimeNanos;

    public double beforeConsumeCpuTokensTimeNanos;

    public boolean isHighContention;


    public double highContentionWithoutLockNanos;

    public boolean limitVirtualScheduler;

    public boolean pinUsingJNA;

    public ConsumeCpuBenchmarkParameters(int threads, LockType lockType, String lockSpec,
                                         boolean isLightThread, long beforeCpuTokens, long inCpuTokens,
                                         int actionsPerThread, double beforeConsumeCpuTokensTimeNanos,
                                         double inConsumeCpuTokensTimeNanos, double highContentionWithoutLockNanos,
                                         int warmupIterations, int measurementIterations, int forks, Map<String, String> profilerParams,
                                         boolean limitVirtualScheduler, boolean pinUsingJNA) {
        super(threads, lockType, actionsPerThread, lockSpec, isLightThread, warmupIterations, measurementIterations, forks, profilerParams);
        this.beforeCpuTokens = beforeCpuTokens;
        this.inCpuTokens = inCpuTokens;
        this.beforeConsumeCpuTokensTimeNanos = beforeConsumeCpuTokensTimeNanos;
        this.inConsumeCpuTokensTimeNanos = inConsumeCpuTokensTimeNanos;
        this.isHighContention = inConsumeCpuTokensTimeNanos * threads > beforeConsumeCpuTokensTimeNanos;
        this.highContentionWithoutLockNanos = highContentionWithoutLockNanos;
        this.limitVirtualScheduler = limitVirtualScheduler;
        this.pinUsingJNA = pinUsingJNA;
    }

    @Override
    public String getBenchmarkName() {
        return String.format(
                "In %d. Before %d. %s contention. Consume CPU. %s threads.",
                inCpuTokens,
                beforeCpuTokens,
                isHighContention ? "High" : "Low",
                isLightThread ? "Virtual" : "Platform"
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
