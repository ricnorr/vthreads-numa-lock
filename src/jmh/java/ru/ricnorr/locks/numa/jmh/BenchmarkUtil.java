package ru.ricnorr.locks.numa.jmh;

import ru.ricnorr.numa.locks.mcs.MCSLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BenchmarkUtil {

    public static Lock initLock(LockType lockType) {
        switch (lockType) {
            case REENTRANT -> {
                return new ReentrantLock();
            }
            case MCS -> {
                return new MCSLock();
            }
            default -> throw new RuntimeException();
        }
    }
}
