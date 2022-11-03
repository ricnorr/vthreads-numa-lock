package ru.ricnorr.numa.locks.mcs;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TestAndSetLock implements Lock {

    private AtomicBoolean flag;

    public TestAndSetLock() {
         flag = new AtomicBoolean(false);
    }

    @Override
    public void lock() {
        while (true) {
            if (flag.compareAndSet(false, true)) {
                return;
            }
        }
    }

    @Override
    public void unlock() {
        flag.set(false);
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
