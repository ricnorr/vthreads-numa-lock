package ru.ricnorr.numa.locks;

import java.util.concurrent.atomic.AtomicInteger;

public class TicketLock extends AbstractLock {

    private AtomicInteger nowServing = new AtomicInteger(Integer.MIN_VALUE);
    private AtomicInteger nextTicket = new AtomicInteger(Integer.MIN_VALUE);

    @Override
    public void lock() {
        int my_ticket = nextTicket.getAndIncrement();
        while (my_ticket != nowServing.get()) {

        }
    }

    @Override
    public void unlock() {
        nowServing.getAndIncrement();
    }
}
