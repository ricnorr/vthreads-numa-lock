package ru.ricnorr.benchmarks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import ru.ricnorr.numa.locks.mcs.MCSLock;
import ru.ricnorr.numa.locks.mcs.MCSYieldLock;
import ru.ricnorr.numa.locks.mcs.TestAndSetLock;
import ru.ricnorr.numa.locks.mcs.TestAndSetYieldLock;
import ru.ricnorr.numa.locks.mcs.TestTestAndSetLock;
import ru.ricnorr.numa.locks.mcs.TestTestAndSetYieldLock;
import ru.ricnorr.numa.locks.mcs.TicketLock;
import ru.ricnorr.numa.locks.mcs.TicketYieldLock;

public class Main {
    private static final List<String> RESULTS_HEADERS = List.of("name", "lock", "threads", "latency", "throughput");

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
            case MCS_YIELD -> {
                return new MCSYieldLock();
            }
            case TEST_SET_YIELD -> {
                return new TestAndSetYieldLock();
            }
            case TEST_TEST_SET_YIELD -> {
                return new TestTestAndSetYieldLock();
            }
            case TICKET_YIELD -> {
                return new TicketYieldLock();
            }
            default -> throw new BenchmarkException("Can't init lockType " + lockType.name());
        }
    }

    private static Runnable createMatrixRunnable(Lock lock, MatrixBenchmarkParameters matrixParam) {
        Random random = new Random();
        int[][] beforeMatrixA = new int[matrixParam.beforeSize][matrixParam.beforeSize];
        int[][] beforeMatrixB = new int[matrixParam.beforeSize][matrixParam.beforeSize];
        for (int i = 0; i < matrixParam.beforeSize; i++) {
            for (int j = 0; j < matrixParam.beforeSize; j++) {
                beforeMatrixA[i][j] = random.nextInt();
                beforeMatrixB[i][j] = random.nextInt();
            }
        }

        int[][] inMatrixA = new int[matrixParam.inSize][matrixParam.inSize];
        int[][] inMatrixB = new int[matrixParam.inSize][matrixParam.inSize];

        for (int i = 0; i < matrixParam.inSize; i++) {
            for (int j = 0; j < matrixParam.inSize; j++) {
                inMatrixA[i][j] = random.nextInt();
                inMatrixB[i][j] = random.nextInt();
            }
        }

        int[][] afterMatrixA = new int[matrixParam.afterSize][matrixParam.afterSize];
        int[][] afterMatrixB = new int[matrixParam.afterSize][matrixParam.afterSize];

        for (int i = 0; i < matrixParam.afterSize; i++) {
            for (int j = 0; j < matrixParam.afterSize; j++) {
                afterMatrixA[i][j] = random.nextInt();
                afterMatrixB[i][j] = random.nextInt();
            }
        }

        return new Runnable() {

            public void multMatrix(int[][] res, int[][] matrixA, int[][] matrixB) {
                for (int i = 0; i < matrixA.length; i++) {
                    for (int j = 0; j < matrixB[0].length; j++) {
                        for (int k = 0; k < matrixA[0].length; k++) {
                            res[i][j] = matrixA[i][k] * matrixB[k][j];
                        }
                    }
                }
            }

            @Override
            public void run() {
                int[][] resBefore = new int[matrixParam.beforeSize][matrixParam.beforeSize];
                int[][] resIn = new int[matrixParam.inSize][matrixParam.inSize];
                int[][] resAfter = new int[matrixParam.afterSize][matrixParam.afterSize];
                multMatrix(resBefore, beforeMatrixA, beforeMatrixB);
                lock.lock();
                multMatrix(resIn, inMatrixA, inMatrixB);
                lock.unlock();
                multMatrix(resAfter, afterMatrixA, afterMatrixB);
            }
        };
    }

    private static void writeResultsToCSVfile(String filename, List<BenchmarkResultsCsv> results) {
        try (FileWriter out = new FileWriter(filename)) {
            try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {
                printer.printRecord(RESULTS_HEADERS);
                results.forEach(it -> {
                    try {
                        printer.printRecord(it.name(), it.lock(), it.threads(), it.latency(), it.throughput());
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
        JSONArray array
    ) {
        List<BenchmarkParameters> paramList = new ArrayList<>();
        for (Object o : array) {
            JSONObject obj = (JSONObject) o;
            String name = (String) obj.get("name");
            for (int thread : threads) {
                for (LockType lockType : lockTypes) {
                    switch (name) {
                        case "matrix" -> {
                            int before = (int) ((long) obj.get("before"));
                            int in = (int) ((long) obj.get("in"));
                            int after = (int) ((long) obj.get("after"));
                            paramList.add(new MatrixBenchmarkParameters(thread, lockType, before, in, after));
                        }
                        default -> throw new BenchmarkException("Unsupported benchmark name");
                    }
                }
            }
        }
        return paramList;
    }

    public static void main(String[] args) {

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
        int durationInMillis = (int) (long) obj.get("durationInMillis");
        long latencyPercentile = ((long) obj.get("latencyPercentile"));
        System.out.println(String.format(
            "Init benchmark params: warmupIterations=%d, iterations=%d,durationInMillis=%d,latencyPercentile=%d",
            warmupIterations,
            iterations,
            durationInMillis,
            latencyPercentile
        ));
        BenchmarkRunner benchmarkRunner =
            new BenchmarkRunner(durationInMillis, warmupIterations, iterations, latencyPercentile);

        JSONArray array = (JSONArray) obj.get("threads");
        List<Integer> threadsList = new ArrayList<>();
        for (Object value : array) {
            threadsList.add((int) ((long) value));
        }
        array = (JSONArray) obj.get("locks");
        List<LockType> locksType = new ArrayList<>();
        for (Object value : array) {
            String lockType = (String) value;
            locksType.add(LockType.valueOf(lockType));
        }
        array = (JSONArray) obj.get("benches");
        List<BenchmarkParameters> benchmarkParametersList = fillBenchmarkParameters(threadsList, locksType, array);

        // Run benches and collect results
        List<BenchmarkResultsCsv> resultCsv = new ArrayList<>();
        for (BenchmarkParameters param : benchmarkParametersList) {
            Lock lock = initLock(param.lockType);
            Runnable benchRunnable;
            if (param instanceof MatrixBenchmarkParameters matrixParam) {
                benchRunnable = createMatrixRunnable(lock, matrixParam);
            } else {
                throw new BenchmarkException("Cannot init runnable for parameter");
            }
            System.out
                .printf("Run bench,name=%s,threads=%d,lock=%s", param.getBenchmarkName(), param.threads, param.lockType.name());
            BenchmarkResult result = benchmarkRunner.benchmark(param.threads, benchRunnable);
            System.out.println("Bench ended");
            resultCsv.add(new BenchmarkResultsCsv(
                param.getBenchmarkName(),
                param.lockType.name(),
                param.threads,
                result.throughput(),
                result.latency()
            ));
        }

        // Print results to file
        writeResultsToCSVfile("results/benchmark_results.csv", resultCsv);
    }
}
