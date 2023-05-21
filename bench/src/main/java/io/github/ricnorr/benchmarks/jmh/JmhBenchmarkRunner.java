package io.github.ricnorr.benchmarks.jmh;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ricnorr.benchmarks.BenchUtils;
import io.github.ricnorr.benchmarks.BenchmarkResultsCsv;
import io.github.ricnorr.benchmarks.Main;
import io.github.ricnorr.benchmarks.params.ConsumeCpuBenchmarkParameters;
import io.github.ricnorr.benchmarks.params.MatrixMultiplicationBenchmarkParameters;
import io.github.ricnorr.benchmarks.params.PriorityQueueBenchmarkParameters;
import io.github.ricnorr.benchmarks.params.TextStatBenchmarkParameter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;

public class JmhBenchmarkRunner {

  public static List<Options> fillBenchmarkParameters(JSONArray array) {
    List<Options> paramList = new ArrayList<>();
    for (Object o : array) {
      JSONObject obj = (JSONObject) o;
      String name = (String) obj.get("name");
      JSONObject payload = (JSONObject) obj.get("payload");
      if (name.equals("consumeCpu")) {
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
        if (consumeCpuBenchmarkParameters.threads == null) {
          consumeCpuBenchmarkParameters.threads = Main.autoThreadsInit();
        }
        paramList.addAll(consumeCpuBenchmarkParameters.getOptions());
      } else if (name.equals("priority-queue")) {
        PriorityQueueBenchmarkParameters priorityQueueBenchmarkParameters;
        try {
          priorityQueueBenchmarkParameters =
              new ObjectMapper().readValue(payload.toJSONString(), PriorityQueueBenchmarkParameters.class);
        } catch (Exception e) {
          throw new RuntimeException("Failed to parse payload of benchmark, err=" + e.getMessage());
        }
        if (priorityQueueBenchmarkParameters.skip) {
          continue;
        }
        if (priorityQueueBenchmarkParameters.threads == null) {
          priorityQueueBenchmarkParameters.threads = Main.autoThreadsInit();
        }
        paramList.addAll(priorityQueueBenchmarkParameters.getOptions());
      } else if (name.equals("text")) {
        TextStatBenchmarkParameter textStatBenchmarkParameter;
        try {
          textStatBenchmarkParameter =
              new ObjectMapper().readValue(payload.toJSONString(), TextStatBenchmarkParameter.class);
        } catch (Exception e) {
          throw new RuntimeException("Failed to parse payload of benchmark, err=" + e.getMessage());
        }
        if (textStatBenchmarkParameter.skip) {
          continue;
        }
        if (textStatBenchmarkParameter.threads == null) {
          textStatBenchmarkParameter.threads = Main.autoThreadsInit();
        }
        paramList.addAll(textStatBenchmarkParameter.getOptions());
      } else if (name.equals("matrix")) {
        MatrixMultiplicationBenchmarkParameters matrixMultiplicationBenchmarkParameters;
        try {
          matrixMultiplicationBenchmarkParameters =
              new ObjectMapper().readValue(payload.toJSONString(), MatrixMultiplicationBenchmarkParameters.class);
        } catch (Exception e) {
          throw new RuntimeException("Failed to parse payload of benchmark, err=" + e.getMessage());
        }
        if (matrixMultiplicationBenchmarkParameters.skip) {
          continue;
        }
        if (matrixMultiplicationBenchmarkParameters.threads == null) {
          matrixMultiplicationBenchmarkParameters.threads = Main.autoThreadsInit();
        }
        paramList.addAll(matrixMultiplicationBenchmarkParameters.getOptions());
      } else {
        throw new IllegalStateException("Benchmark name not found");
      }
    }
    return paramList;
  }

  public static List<Double> runBenchmarkNano(Options options)
      throws RunnerException {
    var res = new Runner(options).run();
    var executionTimesInNanos = new ArrayList<Double>();
    res.forEach(it -> it.getBenchmarkResults().forEach(
        it2 -> it2.getIterationResults()
            .forEach(it3 -> executionTimesInNanos.add(it3.getPrimaryResult().getScore()))));
    return executionTimesInNanos;
  }

  public static BenchmarkResultsCsv runBenchmark(Options options) {
    List<Double> withLocksNanos;
    try {
      withLocksNanos = runBenchmarkNano(options);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.err.println("Error");
      throw new RuntimeException(e);
      //withLocksNanos = List.of(Double.MAX_VALUE);
    }
    if (withLocksNanos.isEmpty()) {
      throw new RuntimeException("No result found in benchmark");
    }
    int threads = Integer.parseInt(options.getParameter("threads").get().stream().findFirst().get());
    var actionsCountParams = options.getParameter("actionsCount");
    int actionsCount = 0;
    if (actionsCountParams.hasValue()) {
      actionsCount = Integer.parseInt(actionsCountParams.get().stream().findFirst().get());
    }
    int warmupIterations = options.getWarmupIterations().get();
    int measurementIterations = options.getMeasurementIterations().get();

    String title =
        String.format("Cores: %d. %s", BenchUtils.CORES_CNT,
            options.getParameter("title").get().stream().findFirst().get());
    String lockType = options.getParameter("lockType").get().stream().findFirst().get();
//    var latenciesNanos = Utils.readLatenciesFromDirectory(warmupIterations + measurementIterations, threads);
//    var medianLatenciesNanos = Utils.medianLatency(warmupIterations, latenciesNanos);
//    var averageLatenciesNanos = Utils.averageLatency(warmupIterations, latenciesNanos);

    var medianLatenciesNanos = 0L;
    var averageLatenciesNanos = 0L;
    double withLockNanosMin = withLocksNanos.stream().min(Double::compare).get();
    double withLockNanosMax = withLocksNanos.stream().max(Double::compare).get();
    double withLockNanosMedian = BenchUtils.median(withLocksNanos);
    double withoutLocksNanos = 0;
    double overheadNanosMin = withLockNanosMin - withoutLocksNanos;
    double overheadNanosMax = withLockNanosMax - withoutLocksNanos;
    double throughputNanosMin = actionsCount / withLockNanosMax;
    double throughputNanosMax = actionsCount / withLockNanosMin;
    double throughputNanosMedian = actionsCount / withLockNanosMedian;
    System.out.printf(
        "Consume cpu bench: i got max_over=%f, min_over=%f, avg_over=%f, max_thrpt=%f, min_thrpt=%f, avg_thrpt=%f%n",
        overheadNanosMax, overheadNanosMin, withLockNanosMedian, throughputNanosMax, throughputNanosMin,
        throughputNanosMedian);
    return new BenchmarkResultsCsv(
        title,
        lockType,
        threads,
        overheadNanosMax, overheadNanosMin, withLockNanosMedian, throughputNanosMax, throughputNanosMin,
        throughputNanosMedian, medianLatenciesNanos, averageLatenciesNanos, BenchUtils.deviation(withLocksNanos));
  }
}
