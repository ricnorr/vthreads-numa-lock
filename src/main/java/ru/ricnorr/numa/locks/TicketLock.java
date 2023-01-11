package ru.ricnorr.numa.locks;

import java.util.concurrent.atomic.AtomicInteger;

import static ru.ricnorr.numa.locks.Utils.spinWaitYield;

public class TicketLock extends AbstractLock {

    private AtomicInteger nowServing = new AtomicInteger(Integer.MIN_VALUE);
    private AtomicInteger nextTicket = new AtomicInteger(Integer.MIN_VALUE);

    @Override
    public void lock() {
        int my_ticket = nextTicket.getAndIncrement();
        int spinCounter = 1;
        while (my_ticket != nowServing.get()) {
            spinCounter = spinWaitYield(spinCounter);
        }
    }

    @Override
    public void unlock() {
        nowServing.getAndIncrement();
    }
}
