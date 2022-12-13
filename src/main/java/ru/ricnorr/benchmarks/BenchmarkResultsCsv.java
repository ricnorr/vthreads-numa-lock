package ru.ricnorr.benchmarks;

record BenchmarkResultsCsv(String name, String lock, int threads, double overheadNanos, double throughputNanos) {
}
