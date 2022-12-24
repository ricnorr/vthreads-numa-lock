package ru.ricnorr.benchmarks;

public record BenchmarkResultsCsv(String name, String lock, int threads, double overheadNanos, double throughputNanos) {
}
