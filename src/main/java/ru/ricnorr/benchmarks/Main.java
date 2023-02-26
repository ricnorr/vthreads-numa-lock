package ru.ricnorr.benchmarks;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import ru.ricnorr.benchmarks.jmh.JmhBenchmarkRunner;
import ru.ricnorr.benchmarks.jmh.cpu.JmhJniCallBenchmark;
import ru.ricnorr.benchmarks.params.BenchmarkParameters;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;
import ru.ricnorr.numa.locks.basic.*;
import ru.ricnorr.numa.locks.cna.nopad.CnaCcl;
import ru.ricnorr.numa.locks.cna.nopad.CnaNuma;
import ru.ricnorr.numa.locks.cna.pad.CnaCclWithContendedPadding;
import ru.ricnorr.numa.locks.cna.pad.CnaNumaWithContendedPadding;
import ru.ricnorr.numa.locks.hclh.nopad.HCLHNuma;
import ru.ricnorr.numa.locks.hclh.nopad.HclhCcl;
import ru.ricnorr.numa.locks.hclh.pad.HCLHNumaPad;
import ru.ricnorr.numa.locks.hclh.pad.HclhCclPad;
import ru.ricnorr.numa.locks.hmcs.nopad.HmcsCclPlusNumaHierarchy;
import ru.ricnorr.numa.locks.hmcs.nopad.HmcsCclPlusNumaPlusSupernumaHierarchy;
import ru.ricnorr.numa.locks.hmcs.nopad.HmcsOnlyCclHierarchy;
import ru.ricnorr.numa.locks.hmcs.nopad.HmcsOnlyNumaHierarchy;
import ru.ricnorr.numa.locks.hmcs.pad.HmcsCclPlusNumaHierarchyPad;
import ru.ricnorr.numa.locks.hmcs.pad.HmcsCclPlusNumaPlusSupernumaHierarchyPad;
import ru.ricnorr.numa.locks.hmcs.pad.HmcsOnlyCclHierarchyPad;
import ru.ricnorr.numa.locks.hmcs.pad.HmcsOnlyNumaHierarchyPad;
import ru.ricnorr.numa.locks.reentrant.NumaReentrantLock;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;

public class Main {

    private static final List<String> RESULTS_HEADERS = List.of("name", "lock", "threads", "Maximum_overhead_(microsec)", "Minimum_overhead_(microsec)", "Median_overhead_(microsec)", "Maximum_throughout_(ops_microsec)", "Minimum_throughput_(ops_microsec)", "Median_throughput_(ops_microsec)");

    public static NumaLock initLock(LockType lockType, String lockSpec, boolean overSubscription, boolean isLight) {
        switch (lockType) {
            case UNFAIR_REENTRANT -> {
                return new NumaReentrantLock(false);
            }
            case FAIR_REENTRANT -> {
                return new NumaReentrantLock(true);
            }
            /**
             * MCS
             */
            case MCS -> {
                return new MCS();
            }
            case MCS_PAD -> {
                return new MCSWithPad();
            }
            case TAS -> {
                return new TestAndSetLock();
            }
            case TTAS -> {
                return new TestTestAndSetLock();
            }
            case TICKET -> {
                return new TicketLock();
            }
            case CLH -> {
                return new CLHLock();
            }
            /**
             * CNA
             */
            case CNA_NUMA -> {
                return new CnaNuma(isLight);
            }
            case CNA_CCL -> {
                return new CnaCcl(isLight);
            }
            case CNA_CCL_PAD -> {
                return new CnaCclWithContendedPadding(isLight);
            }
            case CNA_NUMA_PAD -> {
                return new CnaNumaWithContendedPadding(isLight);
            }
            /**
             * HCLH
             */
            case HCLH_CCL -> {
                return new HclhCcl(isLight);
            }
            case HCLH_NUMA -> {
                return new HCLHNuma(isLight);
            }
            case HCLH_CCL_PAD -> {
                return new HclhCclPad(isLight);
            }
            case HCLH_NUMA_PAD -> {
                return new HCLHNumaPad(isLight);
            }
            /**
             * HMCS
             */
            case HMCS_CCL_NUMA -> {
                return new HmcsCclPlusNumaHierarchy(overSubscription, isLight);
            }
            case HMCS_CCL_NUMA_PAD -> {
                return new HmcsCclPlusNumaHierarchyPad(overSubscription, isLight);
            }
            case HMCS_CCL_NUMA_SUPERNUMA -> {
                return new HmcsCclPlusNumaPlusSupernumaHierarchy(overSubscription, isLight);
            }
            case HMCS_CCL_NUMA_SUPERNUMA_PAD -> {
                return new HmcsCclPlusNumaPlusSupernumaHierarchyPad(overSubscription, isLight);
            }
            case HMCS_CCL -> {
                return new HmcsOnlyCclHierarchy(overSubscription, isLight);
            }
            case HMCS_CCL_PAD -> {
                return new HmcsOnlyCclHierarchyPad(overSubscription, isLight);
            }
            case HMCS_NUMA -> {
                return new HmcsOnlyNumaHierarchy(overSubscription, isLight);
            }
            case HMCS_NUMA_PAD -> {
                return new HmcsOnlyNumaHierarchyPad(overSubscription, isLight);
            }
            default -> throw new BenchmarkException("Can't init lockType " + lockType.name());
        }
    }

    public static NumaLock initLock(LockType lockType, String lockSpec, boolean isLight) {
        return initLock(lockType, lockSpec, false, isLight);
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
                        printer.printRecord(
                                it.name(),
                                it.lock(),
                                it.threads(),
                                it.overheadNanosMax() / 1000,
                                it.overheadNanosMin() / 1000,
                                it.overheadNanosMedian() / 1000,
                                it.throughputNanosMax() * 1000,
                                it.throughputNanosMin() * 1000,
                                it.throughputNanosMedian() * 1000
                        );
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
        // result.add(cpuCount * 2);
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

    public static void estimateJniCall() {
        var optionsBuilder = new OptionsBuilder()
                .include(JmhJniCallBenchmark.class.getSimpleName())
                .operationsPerInvocation(1)
                .warmupIterations(1)
                .forks(1)
                .measurementTime(TimeValue.seconds(5))
                .measurementIterations(1)
                .verbosity(NORMAL);
        try {
            new Runner(optionsBuilder.build()).run();
        } catch (Exception e) {
            throw new BenchmarkException("Can't get jmh benchmark result");
        }
    }

    public static void main(String[] args) throws Throwable {
        if (args.length != 0 && args[0].equals("print-clusters")) {
            printClusters();
            return;
        }
        if (args.length != 0 && args[0].equals("check-jni-call")) {
            estimateJniCall();
            return;
        }
        if (args.length != 0 && args[0].equals("get-carrier")) {
            //Utils.currentCarrierMH.invoke();
            var y = new ThreadLocal<>();
            y.set(1);
            var x = Utils.getByThreadFromThreadLocal(y, Thread.currentThread());
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
        if (type.equals("jmh")) {
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
                throw new RuntimeException("Type not found");
            }

        }

        // Print results to file
        writeResultsToCSVfile("results/benchmark_results.csv", resultCsv);
    }
}
