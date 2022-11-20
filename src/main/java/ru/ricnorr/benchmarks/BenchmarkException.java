package ru.ricnorr.benchmarks;

public class BenchmarkException extends RuntimeException {
    public BenchmarkException(String message, Throwable cause) {
        super(message, cause);
    }

    public BenchmarkException(String message) {
        super(message);
    }
}
