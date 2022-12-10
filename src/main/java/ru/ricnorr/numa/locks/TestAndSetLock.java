package ru.ricnorr.numa.locks;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TestAndSetLock extends AbstractLock {

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
}
