package ru.ricnorr.benchmarks;

public record BenchmarkResultsCsv(
        String name,
        String lock,
        int threads,
        double overheadNanosMax,

        double overheadNanosMin,

        double overheadNanosMedian,
        double throughputNanosMax,
        double throughputNanosMin,
        double throughputNanosMedian
) {
}
