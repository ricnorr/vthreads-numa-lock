package io.github.ricnorr.benchmarks.params;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.ricnorr.benchmarks.BenchUtils;
import io.github.ricnorr.benchmarks.jmh.matrix.JmhMatrixMultiplicationBenchmark;
import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;

public class MatrixMultiplicationBenchmarkParameters implements BenchmarkParameters {

  public Long beforeMatrixSize;

  public Long inMatrixSize;

  public Boolean yieldInCrit;

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
          var options = new OptionsBuilder().include(JmhMatrixMultiplicationBenchmark.class.getSimpleName())
              .warmupIterations(warmupIterations)
              .measurementIterations(measurementIterations)
              .forks(forks)
              .timeout(BENCHMARK_MAX_DURATION)
              .verbosity(NORMAL)
              .jvmArgsAppend("-Djdk.virtualThreadScheduler.parallelism=" +
                  Math.min(BenchUtils.CORES_CNT, thread));
          options = options.param("lockType", lock.name.name());
          options = options.param("beforeMatrixSize", Long.toString(beforeMatrixSize));
          options = options.param("inMatrixSize", Long.toString(inMatrixSize));
          options = options.param("threads", Long.toString(thread));
          options = options.param("yieldInCrit", Boolean.toString(yieldInCrit != null ? yieldInCrit : true));
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
