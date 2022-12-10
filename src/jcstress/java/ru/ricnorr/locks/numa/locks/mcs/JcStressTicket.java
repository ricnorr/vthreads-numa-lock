package ru.ricnorr.locks.numa.locks.mcs;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.infra.results.III_Result;
import ru.ricnorr.numa.locks.TicketLock;

import java.util.concurrent.locks.Lock;

public class JcStressTicket {
    private final Lock lock = new TicketLock();
    private int v;

    @Actor
    public void actor1(III_Result r) {
        lock.lock();
        try {
            r.r1 = ++v;
        } finally {
            lock.unlock();
        }
    }

    @Actor
    public void actor2(III_Result r) {
        lock.lock();
        try {
            r.r2 = ++v;
        } finally {
            lock.unlock();
        }
    }

    @Actor
    public void actor3(III_Result r) {
        lock.lock();
        try {
            r.r3 = ++v;
        } finally {
            lock.unlock();
        }
    }
}
