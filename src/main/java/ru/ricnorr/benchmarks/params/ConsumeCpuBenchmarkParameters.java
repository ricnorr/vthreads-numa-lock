package ru.ricnorr.benchmarks.params;

import java.util.Map;

import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.Utils;

public class ConsumeCpuBenchmarkParameters extends BenchmarkParameters {

  public long beforeCpuTokens;

  public long inCpuTokens;

  public double inConsumeCpuTokensTimeNanos;

  public double beforeConsumeCpuTokensTimeNanos;

  public boolean isHighContention;

  public double highContentionWithoutLockNanos;

  public boolean limitVirtualScheduler;

  public boolean pinUsingJNA;

  public boolean yieldInCrit;

  public int yieldsBefore;


  public ConsumeCpuBenchmarkParameters(int threads, LockType lockType, String lockSpec,
                                       boolean isLightThread, long beforeCpuTokens, long inCpuTokens,
                                       int actionsPerThread, double beforeConsumeCpuTokensTimeNanos,
                                       double inConsumeCpuTokensTimeNanos, double highContentionWithoutLockNanos,
                                       int warmupIterations, int measurementIterations, int forks,
                                       Map<String, String> profilerParams,
                                       boolean limitVirtualScheduler, boolean pinUsingJNA, boolean highCont,
                                       boolean yieldInCrit, String title,
                                       int yieldsBefore) {
    super(threads, lockType, actionsPerThread, lockSpec, isLightThread, warmupIterations, measurementIterations, forks,
        profilerParams, title);
    this.beforeCpuTokens = beforeCpuTokens;
    this.inCpuTokens = inCpuTokens;
    this.beforeConsumeCpuTokensTimeNanos = beforeConsumeCpuTokensTimeNanos;
    this.inConsumeCpuTokensTimeNanos = inConsumeCpuTokensTimeNanos;
    this.isHighContention = highCont;
    this.highContentionWithoutLockNanos = highContentionWithoutLockNanos;
    this.limitVirtualScheduler = limitVirtualScheduler;
    this.pinUsingJNA = pinUsingJNA;
    this.yieldInCrit = yieldInCrit;
    this.yieldsBefore = yieldsBefore;
  }

  @Override
  public String getBenchmarkName() {
    return String.format("Ядер : %d. %s", Utils.CORES_CNT, title);
  }

  public String logBegin() {
    return String.format(
        "%nStart %s contention consume cpu benchmark: threads=%d, lightThreads=%b, " +
            "lockType=%s, lockSpec=%s, beforeTokens=%d, inTokens=%d, " +
            "beforeTimeNanos=%f, inTimeNanos=%f%n",
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
