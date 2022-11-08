package ru.ricnorr.locks.numa.jmh;

import ru.ricnorr.numa.locks.mcs.*;

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
            case MCS_YIELD -> {
                return new MCSYieldLock();
            }
            case TEST_SET -> {
                return new TestAndSetLock();
            }
            case TEST_SET_YIELD -> {
                return new TestAndSetYieldLock();
            }
            case TEST_TEST_SET -> {
                return new TestTestAndSetLock();
            }
            case TEST_TEST_SET_YIELD -> {
                return new TestTestAndSetYieldLock();
            }
            case TICKET -> {
                return new TicketLock();
            }
            case TICKET_YIELD -> {
                return new TicketYieldLock();
            }
            default -> throw new RuntimeException();
        }
    }
}
