package ru.ricnorr.benchmarks.jmh.cpu;

import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;
import static ru.ricnorr.benchmarks.jmh.JmhBenchmarkRunner.runBenchmarkNano;

public class JmhConsumeCpuTokensUtil {
  public static double estimateConsumeCpuTokensTimeNanos(long cpuTokens) throws RunnerException {
    System.out.printf("Run estimate consume %d cpu tokens time nanos%n", cpuTokens);
    var options = new OptionsBuilder().include(JmhConsumeCpuTokensBenchmark.class.getSimpleName())
        .warmupIterations(1)
        .forks(1)
        .measurementIterations(1)
        .measurementTime(TimeValue.seconds(5))
        .warmupTime(TimeValue.seconds(5))
        .verbosity(NORMAL);
    options = options.param("cpuTokens", Long.toString(cpuTokens));
    return runBenchmarkNano(options.build()).stream().findFirst().get();
  }

  public static double estimateHighContentionWithoutLocksTimeNanos(long beforeCpu, long inCpu, long totalActions)
      throws RunnerException {
    System.out.printf(
        "Run estimate high contention without locks time nanos, beforeCpu=%d,inCpu=%d,totalActions=%d%n",
        beforeCpu,
        inCpu,
        totalActions
    );
    var options = new OptionsBuilder().include(JmhSeqConsumeCpuTokensBenchmarkHighContention.class.getSimpleName())
        .warmupIterations(1)
        .forks(1)
        .measurementIterations(1)
        .measurementTime(TimeValue.seconds(5))
        .warmupTime(TimeValue.seconds(5))
        .verbosity(NORMAL);
    options = options.param("beforeCpuTokens", Long.toString(beforeCpu));
    options = options.param("inCpuTokens", Long.toString(inCpu));
    options = options.param("totalActions", Long.toString(totalActions));
    return runBenchmarkNano(options.build()).stream().findFirst().get();
  }
}
