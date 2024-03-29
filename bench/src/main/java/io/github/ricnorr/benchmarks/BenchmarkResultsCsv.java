package io.github.ricnorr.benchmarks;

public record BenchmarkResultsCsv(
    String name,
    String lock,
    int threads,
    double overheadNanosMax,

    double overheadNanosMin,

    double executionTimeMedian,
    double throughputNanosMax,
    double throughputNanosMin,
    double throughputNanosMedian,

    double latencyNanosMedian,

    double latencyNanosAverage,

    double deviation
) {
}
