package ru.ricnorr.benchmarks.params;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import ru.ricnorr.benchmarks.jmh.cpu.JmhParConsumeCpuTokensBenchmark;
import ru.ricnorr.numa.locks.Utils;

import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;

public class ConsumeCpuBenchmarkParameters implements BenchmarkParameters {

  public Long beforeCpuTokens;

  public Long inCpuTokens;

  public Boolean yieldInCrit;

  public Integer yieldsBefore;

  public List<LockParam> locks;

  public Integer threadsFrom;

  public List<Integer> threads;

  public Integer actionsCount;

  public Integer warmupIterations;

  public Integer measurementIterations;

  public Integer forks;

  public Map<String, String> profilerParams = new HashMap<>();

  public String title;

  public boolean skip;

  @Override
  public String getBenchmarkName() {
    return null;
  }

  @Override
  public List<Options> getOptions() {
    if (threadsFrom != null) {
      threads = threads.stream().filter(it -> it >= threadsFrom).collect(Collectors.toList());
    }
    return threads.stream().flatMap(thread -> locks.stream().map(lock -> {
          var options = new OptionsBuilder().include(JmhParConsumeCpuTokensBenchmark.class.getSimpleName())
              .warmupIterations(warmupIterations)
              .measurementIterations(measurementIterations)
              .forks(forks)
              .timeout(TimeValue.valueOf("2m"))
              .verbosity(NORMAL)
              .jvmArgsAppend("-Djdk.virtualThreadScheduler.parallelism=" +
                  Math.min(Utils.CORES_CNT, thread));
          options = options.param("lockType", lock.name.name());
          options = options.param("beforeCpuTokens", Long.toString(beforeCpuTokens));
          options = options.param("inCpuTokens", Long.toString(inCpuTokens));
          options = options.param("threads", Long.toString(thread));
          options = options.param("yieldInCrit", Boolean.toString(yieldInCrit != null ? yieldInCrit : false));
          options = options.param("yieldsBefore", Integer.toString(yieldsBefore != null ? yieldsBefore : 1));
          options = options.param("title", title);
          options = options.param("actionsCount", Integer.toString(actionsCount));
          String asyncProfilerParams = profilerParams.get("async");
          if (asyncProfilerParams != null) {
            System.out.println("Async profiler detected!");
            options.addProfiler(AsyncProfiler.class, asyncProfilerParams);
          }
          String jfrProfilerParams = profilerParams.get("jfr");
          if (jfrProfilerParams != null) {
            System.out.println("JavaFlightRecorder detected!");
            options.addProfiler(JavaFlightRecorderProfiler.class, jfrProfilerParams);
          }
          return options.build();
        })
    ).collect(Collectors.toList());
  }
}
