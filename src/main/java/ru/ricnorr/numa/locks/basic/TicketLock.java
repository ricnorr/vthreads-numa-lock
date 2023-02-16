package ru.ricnorr.numa.locks.basic;

import ru.ricnorr.numa.locks.NumaLock;

import java.util.concurrent.atomic.AtomicInteger;

public class TicketLock implements NumaLock {

    private final AtomicInteger nowServing = new AtomicInteger(Integer.MIN_VALUE);
    private final AtomicInteger nextTicket = new AtomicInteger(Integer.MIN_VALUE);

    @Override
    public Object lock() {
        int my_ticket = nextTicket.getAndIncrement();
        int spinCounter = 1;
        while (my_ticket != nowServing.get()) {
            Thread.onSpinWait();
        }
        return null;
    }

    @Override
    public void unlock(Object t) {
        nowServing.getAndIncrement();
    }
}
