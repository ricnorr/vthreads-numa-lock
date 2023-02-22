package ru.ricnorr.benchmarks.jmh.matrix;

import org.openjdk.jmh.runner.RunnerException;

public class JmhMatrixUtil {
    public static double estimateMatrixMultiplicationTimeNanos(int size) throws RunnerException {
        //return runBenchmarkNano(JmhOneMatrixBenchmark.class, 1, 0, Map.of("matrixSize", Integer.toString(size)));
        return 0;
    }
}
