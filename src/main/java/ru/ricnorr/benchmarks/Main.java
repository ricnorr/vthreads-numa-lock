package ru.ricnorr.benchmarks;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.ejml.concurrency.EjmlConcurrency;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import ru.ricnorr.benchmarks.matrix.MatrixBenchmarkParameters;
import ru.ricnorr.benchmarks.matrix.MatrixBenchmarkUtils;
import ru.ricnorr.numa.locks.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Main {
    private static final List<String> RESULTS_HEADERS = List.of("name", "lock", "threads", "overhead(microsec)", "throughput(ops_microsec)");

    private static Lock initLock(LockType lockType) {
        switch (lockType) {
            case REENTRANT -> {
                return new ReentrantLock();
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
            case CLH -> {
                return new CLHLock();
            }
            case CNA -> {
                return new CNALock();
            }
            default -> throw new BenchmarkException("Can't init lockType " + lockType.name());
        }
    }

    static List<Integer> getProcessorsNumbersInNumaNodeOrder() {
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

    private static List<BenchmarkParameters> fillBenchmarkParameters(
            List<Integer> threads,
            List<LockType> lockTypes,
            JSONArray array,
            int actionsCount
    ) {
        List<BenchmarkParameters> paramList = new ArrayList<>();
        for (Object o : array) {
            JSONObject obj = (JSONObject) o;
            String name = (String) obj.get("name");
            switch (name) {
                case "matrix" -> {
                    int before = (int) ((long) obj.get("before"));
                    int in = (int) ((long) obj.get("in"));
                    double beforeMatrixMultTimeNanos = MatrixBenchmarkUtils.estimateMatrixMultiplicationTimeNanos(before);
                    double inMatrixMultTimeNanos = MatrixBenchmarkUtils.estimateMatrixMultiplicationTimeNanos(in);
                    for (int thread : threads) {
                        for (LockType lockType : lockTypes) {
                            paramList.add(new MatrixBenchmarkParameters(thread, lockType, before, in, actionsCount / thread, beforeMatrixMultTimeNanos, inMatrixMultTimeNanos));
                        }
                    }

                }
                default -> {
                    throw new IllegalStateException("Unknown benchmark type " + name);
                }
            }

        }
        return paramList;
    }

    private static List<Integer> autoThreadsInit() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        List<Integer> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int left = 1 << i;
            int right = 1 << (i + 1);
            int dist = (right - left) / 2;
            threads.add(left);
            if (dist != 0) {
                threads.add(left + dist);
            }
        }
        List<Integer> result = threads.stream().filter(it -> it < cpuCount).collect(Collectors.toList());
        result.add(cpuCount);
        result.add(cpuCount * 2);
        return result;
    }

    private static BenchmarkResultsCsv runBenchmark(BenchmarkRunner runner, BenchmarkParameters param) {
        Lock lock = initLock(param.lockType);
        Runnable withLockRunnable;
        Runnable withoutLockRunnable;
        if (param instanceof MatrixBenchmarkParameters matrixParam) {
            withLockRunnable = MatrixBenchmarkUtils.initMatrixWithLockRunnable(lock, matrixParam);
            withoutLockRunnable = MatrixBenchmarkUtils.initMatrixWithoutLockRunnable(matrixParam);
        } else {
            throw new BenchmarkException("Cannot init runnable for parameter");
        }
        System.out
            .printf(
                "Run bench,name=%s,threads=%d,lock=%s%n",
                param.getBenchmarkName(),
                param.threads,
                param.lockType.name()
            );
        BenchmarkResult result = runner.benchmark(param.threads, param.actionsPerThread, withLockRunnable, withoutLockRunnable);
        System.out.println("Bench ended");
        return new BenchmarkResultsCsv(
            param.getBenchmarkName(),
            param.lockType.name(),
            param.threads,
            result.overheadNanos(),
            result.throughputNanos()
        );
    }

    private static void setAffinity(int threads, long pid, List<Integer> processorsOrderedByNumaOrder) {
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
    public static void main(String[] args) {
        // Don't use concurrency
        EjmlConcurrency.USE_CONCURRENT = false;

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
        System.out.printf(
            "benchmark params: warmupIterations=%d, iterations=%d%n",
            warmupIterations,
            iterations
        );
        BenchmarkRunner benchmarkRunner =
            new BenchmarkRunner(iterations);

        JSONArray array = (JSONArray) obj.get("threads");
        List<Integer> threadsList = new ArrayList<>();
        if (array != null) {
            for (Object value : array) {
                threadsList.add((int) ((long) value));
            }
        } else {
            threadsList = autoThreadsInit();
        }

        array = (JSONArray) obj.get("locks");
        List<LockType> locksType = new ArrayList<>();
        for (Object value : array) {
            String lockType = (String) value;
            locksType.add(LockType.valueOf(lockType));
        }
        array = (JSONArray) obj.get("benches");
        List<BenchmarkParameters> benchmarkParametersList = fillBenchmarkParameters(threadsList, locksType, array, actionsCount);

        // Run benches and collect results
        List<BenchmarkResultsCsv> resultCsv = new ArrayList<>();

        System.out.println("Warmup begin");
        for (int i = 0; i < warmupIterations; i++) {
            runBenchmark(benchmarkRunner, benchmarkParametersList.get(0));
        }
        System.out.println("Warmup end");

        List<Integer> processors = getProcessorsNumbersInNumaNodeOrder();
        System.out.println(processors.stream().map(Object::toString).collect(
                Collectors.joining(",", "Processors ordered by NUMA node\n", "\n"))
        );

        long pid = ProcessHandle.current().pid();
        for (BenchmarkParameters param : benchmarkParametersList) {
            setAffinity(param.threads, pid, processors);
            resultCsv.add(runBenchmark(benchmarkRunner, param));
        }

        // Print results to file
        writeResultsToCSVfile("results/benchmark_results.csv", resultCsv);
    }
}
