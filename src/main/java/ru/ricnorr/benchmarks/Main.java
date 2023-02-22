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
import ru.ricnorr.numa.locks.cna.nopadding.CnaCcl;
import ru.ricnorr.numa.locks.cna.nopadding.CnaNuma;
import ru.ricnorr.numa.locks.cna.padding.CnaCclWithPadding;
import ru.ricnorr.numa.locks.cna.padding.CnaNumaWithPadding;
import ru.ricnorr.numa.locks.hclh.HCLHNuma;
import ru.ricnorr.numa.locks.hclh.HclhCcl;
import ru.ricnorr.numa.locks.hmcs.HmcsCclPlusNumaHierarchy;
import ru.ricnorr.numa.locks.hmcs.HmcsCclPlusNumaPlusSupernumaHierarchy;
import ru.ricnorr.numa.locks.hmcs.HmcsOnlyCclHierarchy;
import ru.ricnorr.numa.locks.hmcs.HmcsOnlyNumaHierarchy;
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

    private static final List<String> RESULTS_HEADERS = List.of("name", "lock", "threads", "mx_overhead(microsec)", "mn_overhead(microsec)", "med_overhead(microsec)", "mx_thrpt(ops_microsec)", "mn_thrpt(ops_microsec)", "med_thrpt(ops_microsec)");

    public static NumaLock initLock(LockType lockType, String lockSpec, boolean overSubscription, boolean isLight) {
        switch (lockType) {
            case UNFAIR_REENTRANT -> {
                return new NumaReentrantLock(false);
            }
            case FAIR_REENTRANT -> {
                return new NumaReentrantLock(true);
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
            case CLH -> {
                return new CLHLock();
            }
            /**
             * CNA
             */
            case CNA_NUMA_NO_PAD -> {
                return new CnaNuma(isLight);
            }
            case CNA_CCL_NO_PAD -> {
                return new CnaCcl(isLight);
            }
            case CNA_CCL_PAD -> {
                return new CnaCclWithPadding(isLight);
            }
            case CNA_NUMA_PAD -> {
                return new CnaNumaWithPadding(isLight);
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
            case MCS -> {
                return new MCS();
            }
            case MCS_WITH_PADDING -> {
                return new MCS_WITH_PADDING();
            }
            case HMCS_CCL_PLUS_NUMA -> {
                return new HmcsCclPlusNumaHierarchy(overSubscription, isLight);
            }
//            case HMCS_CCL_PLUS_NUMA_HIERARCHY_WITH_PADDING -> {
//                return null;
//            }
//
            case HMCS_CCL_PLUS_NUMA_PLUS_SUPERNUMA -> {
                return new HmcsCclPlusNumaPlusSupernumaHierarchy(overSubscription, isLight);
            }
//            case HMCS_CCL_PLUS_NUMA_PLUS_SUPERNUMA_HIERARCHY_WITH_PADDING -> {
//                return null;
//            }
            case HMCS_ONLY_CCL -> {
                return new HmcsOnlyCclHierarchy(overSubscription, isLight);
            }
//            case HMCS_ONLY_CCL_HIERARCHY_WITH_PADDING -> {
//                return null;
//            }
            case HMCS_ONLY_NUMA -> {
                return new HmcsOnlyNumaHierarchy(overSubscription, isLight);
            }
//            case HMCS_ONLY_NUMA_HIERARCHY_WITH_PADDING -> {
//                return null;
//            }

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
