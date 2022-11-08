package ru.ricnorr.numa.locks.mcs;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TicketYieldLock implements Lock {

    private AtomicInteger now_serving = new AtomicInteger(Integer.MIN_VALUE);
    private AtomicInteger next_ticket = new AtomicInteger(Integer.MIN_VALUE);

    @Override
    public void lock() {
        int my_ticket = next_ticket.getAndIncrement();
        while (my_ticket != now_serving.get()) {
            Thread.yield();
        }
    }

    @Override
    public void unlock() {
        now_serving.getAndIncrement();
    }

    @Override
    public void lockInterruptibly() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean tryLock() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean tryLock(long l, @NotNull TimeUnit timeUnit) {
        throw new RuntimeException("Not implemented");
    }

    @NotNull
    @Override
    public Condition newCondition() {
        throw new RuntimeException("Not implemented");
    }
}
