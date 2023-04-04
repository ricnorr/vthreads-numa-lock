package ru.ricnorr.benchmarks.jmh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import ru.ricnorr.benchmarks.BenchmarkResultsCsv;
import ru.ricnorr.benchmarks.jmh.cpu.JmhParConsumeCpuTokensBenchmark;
import ru.ricnorr.benchmarks.params.ConsumeCpuBenchmarkParameters;
import ru.ricnorr.benchmarks.params.LockParam;
import ru.ricnorr.numa.locks.Utils;

import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;
import static ru.ricnorr.benchmarks.Main.autoThreadsInit;

public class JmhBenchmarkRunner {

  public static List<Map<String, String>> fillBenchmarkParameters(List<LockParam> locks, JSONArray array) {
    List<Map<String, String>> paramList = new ArrayList<>();
    for (Object o : array) {
      JSONObject obj = (JSONObject) o;
      String name = (String) obj.get("name");
      if (name.equals("consumeCpu")) {
        JSONObject payload = (JSONObject) obj.get("payload");
        ConsumeCpuBenchmarkParameters consumeCpuBenchmarkParameters;
        try {
          consumeCpuBenchmarkParameters =
              new ObjectMapper().readValue(payload.toJSONString(), ConsumeCpuBenchmarkParameters.class);
        } catch (Exception e) {
          throw new RuntimeException("Failed to parse payload of benchmark, err=" + e.getMessage());
        }
        if (consumeCpuBenchmarkParameters.skip) {
          continue;
        }
        if (consumeCpuBenchmarkParameters.threads.isEmpty()) {
          consumeCpuBenchmarkParameters.threads = autoThreadsInit();
        }
        for (LockParam lock : locks) {
          if (!lock.skip) {
            paramList.addAll(consumeCpuBenchmarkParameters.getMap(lock));
          }
        }
      }
    }
    return paramList;
  }

  public static List<Double> runBenchmarkNano(ChainedOptionsBuilder optionsBuilder, Map<String, String> params)
      throws RunnerException {
    for (Map.Entry<String, String> x : params.entrySet()) {
      optionsBuilder = optionsBuilder.param(x.getKey(), x.getValue());
    }

    var res = new Runner(optionsBuilder.build()).run();
    var executionTimesInNanos = new ArrayList<Double>();
    res.forEach(it -> it.getBenchmarkResults().forEach(
        it2 -> it2.getIterationResults()
            .forEach(it3 -> executionTimesInNanos.add(it3.getPrimaryResult().getScore()))));
    return executionTimesInNanos;
  }

  public static BenchmarkResultsCsv runBenchmark(Map<String, String> parameters) {
    var options = new OptionsBuilder().include(JmhParConsumeCpuTokensBenchmark.class.getSimpleName())
        .warmupIterations(Integer.parseInt(parameters.get("warmupIterations")))
        .measurementIterations(Integer.parseInt(parameters.get("measurementIterations")))
        .forks(Integer.parseInt(parameters.get("forks")))
        .timeout(TimeValue.valueOf("2m"))
        .verbosity(NORMAL)
        .jvmArgsAppend("-Djdk.virtualThreadScheduler.parallelism=" +
            Math.min(Utils.CORES_CNT, Integer.parseInt(parameters.get("threads"))));

    String asyncProfilerParams = parameters.get("async");
    if (asyncProfilerParams != null) {
      System.out.println("Async profiler detected!");
      options.addProfiler(AsyncProfiler.class, asyncProfilerParams);
    }
    String jfrProfilerParams = parameters.get("jfr");
    if (jfrProfilerParams != null) {
      System.out.println("JavaFlightRecorder detected!");
      options.addProfiler(JavaFlightRecorderProfiler.class, jfrProfilerParams);
    }
    List<Double> withLocksNanos;
    try {
      withLocksNanos = runBenchmarkNano(options, parameters);
    } catch (Exception e) {
      withLocksNanos = List.of(Double.MAX_VALUE);
    }
    if (withLocksNanos.isEmpty()) {
      withLocksNanos = List.of(Double.MAX_VALUE);
    }
    double withLockNanosMin = withLocksNanos.stream().min(Double::compare).get();
    double withLockNanosMax = withLocksNanos.stream().max(Double::compare).get();
    double withLockNanosMedian = Utils.median(withLocksNanos);
    double withoutLocksNanos;
    boolean isHighContention = Boolean.parseBoolean(parameters.get("isHighContention"));
    if (isHighContention) {
      withoutLocksNanos = Double.parseDouble(parameters.get("beforeConsumeCpuTokensTimeNanos"));
    } else {
      withoutLocksNanos = 0;
    }
    int threads = Integer.parseInt(parameters.get("threads"));
    int actionsPerThread = Integer.parseInt(parameters.get("actionsPerThread"));
    double overheadNanosMin = withLockNanosMin - withoutLocksNanos;
    double overheadNanosMax = withLockNanosMax - withoutLocksNanos;
    double overheadNanosAverage = withLockNanosMedian - withoutLocksNanos;
    double throughputNanosMin = (threads * actionsPerThread) / withLockNanosMax;
    double throughputNanosMax = (threads * actionsPerThread) / withLockNanosMin;
    double throughputNanosMedian = (threads * actionsPerThread) / withLockNanosMedian;
    System.out.printf(
        "Consume cpu bench: i got max_over=%f, min_over=%f, avg_over=%f, max_thrpt=%f, min_thrpt=%f, avg_thrpt=%f%n",
        overheadNanosMax, overheadNanosMin, overheadNanosAverage, throughputNanosMax, throughputNanosMin,
        throughputNanosMedian);
    return new BenchmarkResultsCsv(
        parameters.get("title"),
        parameters.get("lockType"),
        threads,
        overheadNanosMax, overheadNanosMin, overheadNanosAverage, throughputNanosMax, throughputNanosMin,
        throughputNanosMedian);
  }
}
