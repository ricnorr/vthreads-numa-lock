package ru.ricnorr.benchmarks;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openjdk.jmh.runner.RunnerException;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import ru.ricnorr.benchmarks.custom.CustomBenchmarkRunner;
import ru.ricnorr.benchmarks.jmh.JmhBenchmarkRunner;
import ru.ricnorr.benchmarks.params.BenchmarkParameters;
import ru.ricnorr.numa.locks.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Main {
    private static final List<String> RESULTS_HEADERS = List.of("name", "lock", "threads", "overhead(microsec)", "throughput(ops_microsec)");

    public static Lock initLock(LockType lockType, String lockSpec) {
        switch (lockType) {
            case UNFAIR_REENTRANT -> {
                return new ReentrantLock(false);
            }
            case FAIR_REENTRANT -> {
                return new ReentrantLock(true);
            }
            case MCS -> {
                return new MCSLock();
            }
            case TEST_SET -> {
                return new TestAndSetLock();
            }
            case TEST_TEST_SET -> {
                return new TestTestAndSetLock();
            }
            case TICKET -> {
                return new TicketLock();
            }
            case HCLH -> {
                return new HCLHLock();
            }
            case HCLH_CCL_SPLIT -> {
                return new HCLHCCLSplitLock();
            }
            case CLH -> {
                return new CLHLock();
            }
            case CNA -> {
                return new CNALock(new CnaLockSpec(lockSpec));
            }
            case MCS_NO_PARK -> {
                return new MCSNoParkLock();
            }
            case HMCS -> {
                return new HMCS(new HMCSLockSpec(lockSpec));
            }
            case HCLH_CCL_SPLIT_BACKOFF -> {
                return new HCLHCCLSplitWithBackoffLock(new HCLHCCLSplitWithBackoffLock.HCLHCCLSplitWithBackoffLockSpec(lockSpec));
            }
            case HMCS_PARK -> {
                return new HMCS_PARK(new HMCSLockSpec(lockSpec));
            }
            case HMCS_PARK_V2 -> {
                return new HMCS_PARK_V2(new HMCSLockSpec(lockSpec));
            }
            case HMCS_PARK_V3 -> {
                return new HMCS_PARK_V3(new HMCSLockSpec(lockSpec));
            }
            case HMCS_PARK_V4 -> {
                return new HMCS_PARK_V4(new HMCSLockSpec(lockSpec));
            }
            case HMCS_PARK_V5 -> {
                return new HMCS_PARK_V5(new HMCSLockSpec(lockSpec));
            }
            case HMCS_PARK_V6 -> {
                return new HMCS_PARK_V6(new HMCSLockSpec(lockSpec));
            }
            default -> throw new BenchmarkException("Can't init lockType " + lockType.name());
        }
    }

    public static List<Integer> getProcessorsNumbersInNumaNodeOrder() {
        SystemInfo si = new SystemInfo();
        var logicalProcessors = si.getHardware().getProcessor().getLogicalProcessors();
        return logicalProcessors.stream().sorted(
                Comparator.comparing(CentralProcessor.LogicalProcessor::getNumaNode)
                        .thenComparing(CentralProcessor.LogicalProcessor::getProcessorNumber)
        ).map(CentralProcessor.LogicalProcessor::getProcessorNumber).collect(Collectors.toList());
    }

    private static void writeResultsToCSVfile(String filename, List<BenchmarkResultsCsv> results) {
        try (FileWriter out = new FileWriter(filename)) {
            try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {
                printer.printRecord(RESULTS_HEADERS);
                results.forEach(it -> {
                    try {
                        printer.printRecord(it.name(), it.lock(), it.threads(), it.overheadNanos() / 1000, it.throughputNanos() * 1000);
                    } catch (IOException e) {
                        throw new BenchmarkException("Cannot write record to file with benchmarks results", e);
                    }
                });
            }
        } catch (IOException e) {
            throw new BenchmarkException("Cannot write to file with benchmarks results", e);
        }
    }

    private static List<Integer> autoThreadsInit() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        List<Integer> threads = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            threads.add(1 << i);
        }
        List<Integer> result = threads.stream().filter(it -> it < cpuCount).collect(Collectors.toList());
        result.add(cpuCount);
        result.add(cpuCount * 2);
        return result;
    }


    public static void setAffinity(int threads, long pid, List<Integer> processorsOrderedByNumaOrder) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<Integer> processorsToUse = processorsOrderedByNumaOrder.subList(0, Math.min(processorsOrderedByNumaOrder.size(), threads));
        String cpuList = processorsToUse.stream().map(Object::toString).collect(Collectors.joining(","));
        processBuilder.command("taskset", "-cp", cpuList, Long.toString(pid));
        try {

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

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
            System.out.printf("Proc number: %d, Proc physical number: %d, Proc numa node: %d, Proc group: %d, Proc phys package: %d%n",
                    logicalProcessor.getProcessorNumber(),
                    logicalProcessor.getPhysicalProcessorNumber(),
                    logicalProcessor.getNumaNode(),
                    logicalProcessor.getProcessorGroup(),
                    logicalProcessor.getPhysicalPackageNumber()
            );
        }
        for (int i = 0; i < Runtime.getRuntime().availableProcessors() * 2; i++) {
            threads.add(new Thread(() -> {
                numaNodes.add(Utils.getClusterID());
                cpuIds.add(Utils.getCpuID());
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
        System.out.println("Possible numa nodes: " + numaNodes.stream().sorted().distinct().map(Object::toString).collect(Collectors.joining(",")));
        System.out.println("Possible cpu ids: " + cpuIds.stream().sorted().distinct().map(Object::toString).collect(Collectors.joining(",")));
    }

    public static void main(String[] args) throws RunnerException {
        if (args.length != 0 && args[0].equals("print-clusters")) {
            printClusters();
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
        int warmupIterations = (int) ((long) obj.get("warmupIterations"));
        int iterations = (int) ((long) obj.get("iterations"));
        int actionsCount = (int) ((long) obj.get("actionsCount"));
        String type = (String) obj.get("type");
        System.out.printf(
                "benchmark params: warmupIterations=%d, iterations=%d, type=%s%n",
                warmupIterations,
                iterations,
                type
        );

        JSONArray array = (JSONArray) obj.get("threads");
        List<Integer> threadsList = new ArrayList<>();
        if (array != null) {
            for (Object value : array) {
                threadsList.add((int) ((long) value));
            }
        } else {
            threadsList = autoThreadsInit();
        }

        var locks = (JSONArray) obj.get("locks");
        var benches = (JSONArray) obj.get("benches");
        List<BenchmarkParameters> benchmarkParametersList;
        if (type.equals("custom")) {
            benchmarkParametersList = CustomBenchmarkRunner.fillBenchmarkParameters(threadsList, null, benches, actionsCount);
        } else if (type.equals("jmh")) {
            benchmarkParametersList = JmhBenchmarkRunner.fillBenchmarkParameters(threadsList, locks, benches, actionsCount);
        } else {
            throw new BenchmarkException("Illegal benchmark type");
        }

        // Run benches and collect results
        List<BenchmarkResultsCsv> resultCsv = new ArrayList<>();

        List<Integer> processors = getProcessorsNumbersInNumaNodeOrder();
        System.out.println(processors.stream().map(Object::toString).collect(
                Collectors.joining(",", "Processors ordered by NUMA node\n", "\n"))
        );

        for (BenchmarkParameters param : benchmarkParametersList) {
            if (type.equals("jmh")) {
                resultCsv.add(JmhBenchmarkRunner.runBenchmark(iterations, warmupIterations, param));
            } else {
                resultCsv.add(CustomBenchmarkRunner.runBenchmark(iterations, param));
            }

        }

        // Print results to file
        writeResultsToCSVfile("results/benchmark_results.csv", resultCsv);
    }
}
