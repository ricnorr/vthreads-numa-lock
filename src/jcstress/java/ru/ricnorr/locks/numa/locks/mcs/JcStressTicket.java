package ru.ricnorr.locks.numa.locks.mcs;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.infra.results.III_Result;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.basic.Ticket;

public class JcStressTicket {
    private final NumaLock lock = new Ticket();
    private int v;

    @Actor
    public void actor1(III_Result r) {
        lock.lock();
        try {
            r.r1 = ++v;
        } finally {
            lock.unlock(null);
        }
    }

    @Actor
    public void actor2(III_Result r) {
        lock.lock();
        try {
            r.r2 = ++v;
        } finally {
            lock.unlock(null);
        }
    }

    @Actor
    public void actor3(III_Result r) {
        lock.lock();
        try {
            r.r3 = ++v;
        } finally {
            lock.unlock(null);
        }
    }
}
