package ru.ricnorr.benchmarks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import ru.ricnorr.benchmarks.jmh.JmhBenchmarkRunner;
import ru.ricnorr.benchmarks.jmh.cpu.JmhJniCallBenchmark;
import ru.ricnorr.numa.locks.Utils;

import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;

public class Main {

  private static final List<String> RESULTS_HEADERS =
      List.of("name", "lock", "threads", "Maximum_overhead_(millisec)", "Minimum_overhead_(millisec)",
          "Median_overhead_(millisec)", "Maximum_throughout_(ops_millisec)", "Minimum_throughput_(ops_millisec)",
          "Пропускная способность (op|ms)", "Медиана максимальных latency (millisec)",
          "Среднее максимальных latency (millisec)");

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
        resultsCsv.overheadNanosMin() / 1000 / 1000, resultsCsv.overheadNanosMedian() / 1000 / 1000,
        resultsCsv.throughputNanosMax() * 1000 * 1000, resultsCsv.throughputNanosMin() * 1000 * 1000,
        resultsCsv.throughputNanosMedian() * 1000 * 1000, resultsCsv.latencyNanosMedian() / 1000 / 1000,
        resultsCsv.latencyNanosAverage() / 1000 / 1000);
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

  public static void printClusters() {
    List<Thread> threads = new ArrayList<>();
    Deque<Integer> numaNodes = new ConcurrentLinkedDeque<>();
    Deque<Integer> cpuIds = new ConcurrentLinkedDeque<>();

    SystemInfo si = new SystemInfo();
    var logicalProcessors = si.getHardware().getProcessor().getLogicalProcessors();
    for (CentralProcessor.LogicalProcessor logicalProcessor : logicalProcessors) {
      System.out.printf(
          "Proc number: %d, Proc physical number: %d, Proc numa node: %d, Proc group: %d, Proc phys package: %d%n",
          logicalProcessor.getProcessorNumber(), logicalProcessor.getPhysicalProcessorNumber(),
          logicalProcessor.getNumaNode(), logicalProcessor.getProcessorGroup(),
          logicalProcessor.getPhysicalPackageNumber());
    }
    for (int i = 0; i < Runtime.getRuntime().availableProcessors() * 2; i++) {
      threads.add(new Thread(() -> {
        numaNodes.add(Utils.getNumaNodeId());
        cpuIds.add(Utils.getCpuId());
      }));
    }
    for (Thread thread : threads) {
      thread.start();
    }
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    System.out.println("Possible numa nodes: " +
        numaNodes.stream().sorted().distinct().map(Object::toString).collect(Collectors.joining(",")));
    System.out.println("Possible cpu ids: " +
        cpuIds.stream().sorted().distinct().map(Object::toString).collect(Collectors.joining(",")));
  }

  public static void estimateJniCall() {
    var optionsBuilder =
        new OptionsBuilder().include(JmhJniCallBenchmark.class.getSimpleName()).operationsPerInvocation(1)
            .warmupIterations(1).forks(1).measurementTime(TimeValue.seconds(5)).measurementIterations(1)
            .verbosity(NORMAL);
    try {
      new Runner(optionsBuilder.build()).run();
    } catch (Exception e) {
      throw new BenchmarkException("Can't get jmh benchmark result");
    }
  }

  public static void main(String[] args) {
    if (args.length != 0 && args[0].equals("print-clusters")) {
      printClusters();
      return;
    }
    if (args.length != 0 && args[0].equals("check-jni-call")) {
      estimateJniCall();
      return;
    }
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
