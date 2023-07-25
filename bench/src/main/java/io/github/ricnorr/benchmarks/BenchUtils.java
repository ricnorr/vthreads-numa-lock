package io.github.ricnorr.benchmarks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.github.ricnorr.numa_locks.CLH;
import io.github.ricnorr.numa_locks.CNA;
import io.github.ricnorr.numa_locks.HCLH;
import io.github.ricnorr.numa_locks.HMCSCcl;
import io.github.ricnorr.numa_locks.HMCSCclNuma;
import io.github.ricnorr.numa_locks.HMCSCclNumaSupernuma;
import io.github.ricnorr.numa_locks.HMCSNuma;
import io.github.ricnorr.numa_locks.HMCSNumaSupernuma;
import io.github.ricnorr.numa_locks.LockUtils;
import io.github.ricnorr.numa_locks.MCS;
import io.github.ricnorr.numa_locks.NumaMCS;
import io.github.ricnorr.numa_locks.NumaReentrantLock;
import io.github.ricnorr.numa_locks.TAS;
import io.github.ricnorr.numa_locks.TTAS;
import io.github.ricnorr.numa_locks.Ticket;
import io.github.ricnorr.numa_locks.VNA_2_Q;
import io.github.ricnorr.numa_locks.VthreadNumaLock;
import org.apache.commons.io.FileUtils;

public class BenchUtils {

    public static int CORES_CNT = Runtime.getRuntime().availableProcessors();


    /**
     * JMH не позволяет передавать кастомный результат, поэтому бенчмарк записывает в директорию результаты latency
     */
    public static String LATENCIES_DIR_NAME = "latencies";


    public static VthreadNumaLock initLock(LockType lockType, int threads) {
        switch (lockType) {
            // Standard locks
            case UNFAIR_REENTRANT -> {
                return new NumaReentrantLock(false);
            }
            case FAIR_REENTRANT -> {
                return new NumaReentrantLock(true);
            }
            case MCS -> {
                return new MCS();
            }
            case TAS -> {
                return new TAS();
            }
            case TTAS -> {
                return new TTAS();
            }
            case TICKET -> {
                return new Ticket();
            }
            case CLH -> {
                return new CLH();
            }
            // CNA
            case CNA_Q -> {
                return new CNA(LockUtils::getNumaNodeId, false);
            }
            case CNA_QSPIN -> {
                return new CNA(LockUtils::getNumaNodeId, true);
            }
            // HCLH
            case HCLH_CCL -> {
                return new HCLH(LockUtils::getKunpengCCLId);
            }
            case HCLH_NUMA -> {
                return new HCLH(LockUtils::getNumaNodeId);
            }
            // HMCS
            case HMCS_Q -> {
                return new HMCSNuma(false);
            }
            case HMCS_QSPIN -> {
                return new HMCSNuma(true);
            }
            case HMCS_CCL_NUMA -> {
                return new HMCSCclNuma(false);
            }
            case HMCS_CCL_NUMA_SUPERNUMA -> {
                return new HMCSCclNumaSupernuma(false);
            }
            case HMCS_CCL -> {
                return new HMCSCcl(false);
            }
            case HMCS_NUMA -> {
                return new HMCSNuma(false);
            }
            case HMCS_NUMA_SUPERNUMA -> {
                return new HMCSNumaSupernuma(false);
            }
            case VNA -> {
                return new NumaMCS();
            }
            case VNA_2_Q -> {
                return new VNA_2_Q();
            }
            default -> throw new BenchmarkException("Can't init lockType " + lockType.name());
        }
    }

    public static void pinVirtualThreadsToCores(int cores) {
        System.out.println("Pin virtual threads to cores");
        ThreadFactory threadFactory = Thread.ofVirtual().factory();
        AtomicInteger customBarrier = new AtomicInteger();
        AtomicBoolean locked = new AtomicBoolean(false);
        var carrierThreads = new HashSet<>();
        Phaser phaser = new Phaser(cores + 1);
        for (int i = 0; i < cores; i++) {
            threadFactory.newThread(
                    () -> {
                        customBarrier.incrementAndGet();
                        while (customBarrier.get() < cores) {
                            // do nothing
                        }
                        while (!locked.compareAndSet(false, true)) {
                            // do nothing
                        }
                        Thread currentCarrier = LockUtils.getCurrentCarrierThread();
                        if (!carrierThreads.contains(currentCarrier)) {
                            Affinity.affinityLib.pinToCore(carrierThreads.size());
                            carrierThreads.add(currentCarrier);
                        }
                        locked.compareAndSet(true, false);
                        phaser.arrive();
                    }
            ).start();
        }
        phaser.arriveAndAwaitAdvance();
        System.out.println("Pinned virtual threads to cores");
    }

    public static VthreadNumaLock initLock(LockType lockType) {
        return initLock(lockType, 0);
    }


    public static List<List<List<Long>>> readLatenciesFromDirectory(int totalIterations, int threads) {
        try {
            List<List<List<Long>>> latencies = new ArrayList<>();
            for (int iteration = 0; iteration < totalIterations; iteration++) {
                var iterationLatencies = new ArrayList<List<Long>>();
                latencies.add(iterationLatencies);
                for (int threadNum = 0; threadNum < threads; threadNum++) {
                    var threadLatencies = new ArrayList<Long>();
                    iterationLatencies.add(threadLatencies);
                    var path = Paths.get(String.format("%s/%d_%d.tmp", LATENCIES_DIR_NAME, iteration, threadNum));
                    threadLatencies.addAll(Files.readAllLines(path).stream().map(Long::parseLong).toList());
                }
            }
            FileUtils.deleteDirectory(new File(LATENCIES_DIR_NAME));
            return latencies;
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    public static double median(Collection<Double> numbers) {
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute median on empty collection of numbers");
        }
        List<Double> numbersList = new ArrayList<>(numbers);
        Collections.sort(numbersList);
        int middle = numbersList.size() / 2;
        if (numbersList.size() % 2 == 0) {
            return 0.5 * (numbersList.get(middle) + numbersList.get(middle - 1));
        } else {
            return numbersList.get(middle);
        }
    }

    public static double deviation(Collection<Double> numbers) {
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute deviation on empty collection of numbers");
        }
        double mean = numbers.stream().mapToDouble(it -> it).sum();
        double deviation = 0;
        for (Double number : numbers) {
            deviation += Math.pow(number - mean, 2);
        }
        return Math.sqrt(deviation);
    }

    public static double medianLatency(int warmupIterations, List<List<List<Long>>> latencies) {
        List<Long> allLatencies = new ArrayList<>();
        for (int i = warmupIterations; i < latencies.size(); i++) {
            var iterationLatencies = latencies.get(warmupIterations);
            var maxLatenciesForEachThreadOnIteration =
                    iterationLatencies.stream().map(it -> it.stream().mapToLong(it2 -> it2).max().getAsLong()).toList();
            allLatencies.addAll(maxLatenciesForEachThreadOnIteration);
        }
        return BenchUtils.median(allLatencies.stream().map(it -> (double) it).collect(Collectors.toList()));
    }

    public static double averageLatency(int warmupIterations, List<List<List<Long>>> latencies) {
        List<Long> allLatencies = new ArrayList<>();
        for (int i = warmupIterations; i < latencies.size(); i++) {
            var iterationLatencies = latencies.get(warmupIterations);
            var maxLatenciesForEachThreadOnIteration =
                    iterationLatencies.stream().map(it -> it.stream().mapToLong(it2 -> it2).max().getAsLong()).toList();
            allLatencies.addAll(maxLatenciesForEachThreadOnIteration);
        }
        return allLatencies.stream().mapToDouble(it -> (double) it).average().getAsDouble();
    }
}
