package ru.ricnorr.numa.locks.basic;

import ru.ricnorr.numa.locks.NumaLock;

import java.util.concurrent.atomic.AtomicInteger;

public class Ticket implements NumaLock {

    private final AtomicInteger nowServing = new AtomicInteger(Integer.MIN_VALUE);
    private final AtomicInteger nextTicket = new AtomicInteger(Integer.MIN_VALUE);

    @Override
    public Object lock(Object obj) {
        int myTicket = nextTicket.getAndIncrement();
        while (myTicket != nowServing.get()) {
            Thread.onSpinWait();
        }
        return null;
    }

    @Override
    public void unlock(Object t) {
        nowServing.getAndIncrement();
    }

    @Override
    public boolean hasNext(Object obj) {
        return nextTicket.get() > nowServing.get() + 1;
    }
}
