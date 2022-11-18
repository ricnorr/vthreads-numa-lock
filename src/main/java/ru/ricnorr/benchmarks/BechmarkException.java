package ru.ricnorr.benchmarks;

public class BechmarkException extends RuntimeException {
    public BechmarkException(String message, Throwable cause) {
        super(message, cause);
    }

    public BechmarkException(String message) {
        super(message);
    }
}
