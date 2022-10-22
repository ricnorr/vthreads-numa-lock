package ru.ricnorr.locks.numa.jmh;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BenchmarkUtil {

    public static Lock initLock(LockType lockType) {
        switch (lockType) {
            case REENTRANT_LOCK -> {
                return new ReentrantLock();
            }
            default -> throw new RuntimeException();
        }
    }
}
