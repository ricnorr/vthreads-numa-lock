package ru.ricnorr.benchmarks.jmh.matrix;

import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.ricnorr.benchmarks.BenchmarkException;

import java.util.Map;

import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;
import static org.openjdk.jmh.runner.options.VerboseMode.SILENT;
import static ru.ricnorr.benchmarks.jmh.JmhBenchmarkRunner.runBenchmarkNano;

public class JmhMatrixUtil {
    public static double estimateMatrixMultiplicationTimeNanos(int size) throws RunnerException {
        return runBenchmarkNano(JmhOneMatrixBenchmark.class, 1, 0, Map.of("matrixSize", Integer.toString(size)));
    }
}
