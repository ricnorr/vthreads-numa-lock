package ru.ricnorr.locks.numa.locks.mcs;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.III_Result;
import ru.ricnorr.benchmarks.BenchmarkParameters;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.CNALock;
import ru.ricnorr.numa.locks.CnaLockSpec;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

@JCStressTest
@Outcome(id = {"1, 2, 3", "2, 1, 3", "1, 3, 2", "2, 3, 1", "3, 1, 2", "3, 2, 1"}, expect = ACCEPTABLE, desc = "Mutex works")
@State
public class JcStressCNA {

    private final CNALock lock = new CNALock(new CnaLockSpec(""));
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

