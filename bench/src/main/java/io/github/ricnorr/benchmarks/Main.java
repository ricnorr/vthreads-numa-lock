package io.github.ricnorr.benchmarks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.github.ricnorr.benchmarks.jmh.JmhBenchmarkRunner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openjdk.jmh.runner.options.Options;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class Main {

  private static final List<String> RESULTS_HEADERS =
      List.of("name", "lock", "threads", "Maximum_overhead_(millisec)", "Minimum_overhead_(millisec)",
          "Median of execution time (ms)", "Maximum_throughout_(ops_millisec)", "Minimum_throughput_(ops_millisec)",
          "Throughput (op|ms)", "Медиана максимальных latency (millisec)",
          "Среднее максимальных latency (millisec)", "Deviation (ms)");

  public static List<Integer> getProcessorsNumbersInNumaNodeOrder() {
    SystemInfo si = new SystemInfo();
    var logicalProcessors = si.getHardware().getProcessor().getLogicalProcessors();
    return logicalProcessors.stream().sorted(Comparator.comparing(CentralProcessor.LogicalProcessor::getNumaNode)
            .thenComparing(CentralProcessor.LogicalProcessor::getProcessorNumber))
        .map(CentralProcessor.LogicalProcessor::getProcessorNumber).collect(Collectors.toList());
  }

  private static void print(CSVPrinter printer, BenchmarkResultsCsv resultsCsv) throws IOException {
    printer.printRecord(resultsCsv.name(), resultsCsv.lock(), resultsCsv.threads(),
        resultsCsv.overheadNanosMax() / 1000 / 1000,
        resultsCsv.overheadNanosMin() / 1000 / 1000,
        resultsCsv.executionTimeMedian() / 1000 / 1000,
        resultsCsv.throughputNanosMax() * 1000 * 1000, resultsCsv.throughputNanosMin() * 1000 * 1000,
        resultsCsv.throughputNanosMedian() * 1000 * 1000, resultsCsv.latencyNanosMedian() / 1000 / 1000,
        resultsCsv.latencyNanosAverage() / 1000 / 1000,
        resultsCsv.deviation() / 1000 / 1000);
  }

  private static void writeResultsToCSVfile(String filename, List<BenchmarkResultsCsv> results) {
    try (FileWriter out = new FileWriter(filename)) {
      try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {
        printer.printRecord(RESULTS_HEADERS);
        results.forEach(it -> {
          try {
            print(printer, it);
            // bug with matplotlib - last point should be duplicated
            if (results.stream().filter(result -> result.name().equals(it.name()))
                .allMatch(result -> result.threads() <= it.threads())) {
              print(printer, it);
            }
          } catch (IOException e) {
            throw new BenchmarkException("Cannot write record to file with benchmarks results", e);
          }

        });
      }
    } catch (IOException e) {
      throw new BenchmarkException("Cannot write to file with benchmarks results", e);
    }
  }

  public static List<Integer> autoThreadsInit() {
    int cores = Runtime.getRuntime().availableProcessors();
    if (cores == 128) {
      return List.of(4, 16, 32, 64, 96, 128, 128 * 2, 128 * 5);
    }
    if (cores == 48) {
      return List.of(4, 16, 24, 36, 48, 48 * 2, 48 * 5);
    }
    List<Integer> threadsList =
        new ArrayList<>(List.of(4, 8, 16, 24, 32, 48, 64, 80, 96, 128)).stream().filter(it -> it < cores)
            .collect(Collectors.toList());
    threadsList.addAll(
        List.of(cores, cores + (cores / 4), cores + (cores / 2), 2 * cores, 4 * cores, 8 * cores, 10 * cores,
            20 * cores));
    return threadsList.stream().distinct().toList();
  }

  public static void setAffinity(int threads, long pid, List<Integer> processorsOrderedByNumaOrder) {
    ProcessBuilder processBuilder = new ProcessBuilder();
    List<Integer> processorsToUse =
        processorsOrderedByNumaOrder.subList(0, Math.min(processorsOrderedByNumaOrder.size(), threads));
    String cpuList = processorsToUse.stream().map(Object::toString).collect(Collectors.joining(","));
    processBuilder.command("taskset", "-cp", cpuList, Long.toString(pid));
    try {

      Process process = processBuilder.start();

      StringBuilder output = new StringBuilder();

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }

      int exitVal = process.waitFor();
      System.out.println();
      System.out.println(output);

      if (exitVal != 0) {
        System.out.println(output);
        throw new BenchmarkException("Set affinity - fail");
      }
    } catch (IOException | InterruptedException e) {
      throw new BenchmarkException("Set affinity - fail", e);
    }
  }

  public static void main(String[] args) throws InterruptedException {
    // Read benchmark parameters
    String s;

    try {
      s = FileUtils.readFileToString(new File("settings/settings.json"), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new BenchmarkException("Cannot read input file", e);
    }
    JSONObject obj = (JSONObject) JSONValue.parse(s);
    var benches = (JSONArray) obj.get("benches");
    List<Options> benchmarkOptions = JmhBenchmarkRunner.fillBenchmarkParameters(benches);

    // Run benches and collect results
    List<BenchmarkResultsCsv> resultCsv = new ArrayList<>();

    List<Integer> processors = getProcessorsNumbersInNumaNodeOrder();
    System.out.println(processors.stream().map(Object::toString)
        .collect(Collectors.joining(",", "Processors ordered by NUMA node\n", "\n")));

    for (Options option : benchmarkOptions) {
      resultCsv.add(JmhBenchmarkRunner.runBenchmark(option));

    }

    // Print results to file
    writeResultsToCSVfile("results/benchmark_results.csv", resultCsv);
  }
}
