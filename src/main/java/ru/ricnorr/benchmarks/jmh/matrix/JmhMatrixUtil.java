package ru.ricnorr.benchmarks.jmh.matrix;

import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.ricnorr.benchmarks.BenchmarkException;

import java.util.Map;

import static org.openjdk.jmh.runner.options.VerboseMode.NORMAL;
import static org.openjdk.jmh.runner.options.VerboseMode.SILENT;

public class JmhMatrixUtil {
    public static double estimateMatrixMultiplicationTimeNanos(int size) throws RunnerException {
        return runBenchmarkNano(JmhOneMatrixBenchmark.class, 1, 0, Map.of("matrixSize", Integer.toString(size)));
    }

    public static double runBenchmarkNano(Class<?> clazz, int iterations, int warmupIterations, Map<String, String> params) throws RunnerException {
        var optionsBuilder = new OptionsBuilder()
                .include(clazz.getSimpleName())
                .operationsPerInvocation(1)
                .warmupIterations(warmupIterations)
                .forks(1)
                .addProfiler("async")
                .measurementIterations(iterations)
                .verbosity(SILENT);
        for (Map.Entry<String, String> x : params.entrySet()) {
            optionsBuilder = optionsBuilder.param(x.getKey(), x.getValue());
        }

        var res = new Runner(optionsBuilder.build()).run();
        for (BenchmarkResult x : res.stream().findFirst().get().getBenchmarkResults()) {
            assert(x.getPrimaryResult().getScoreUnit().equals("ns/op"));
            return x.getPrimaryResult().getScore();
        }
        throw new BenchmarkException("Can't get jmh benchmark result");
    }
}
