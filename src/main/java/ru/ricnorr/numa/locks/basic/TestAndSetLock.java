package ru.ricnorr.numa.locks.basic;

import ru.ricnorr.numa.locks.NumaLock;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestAndSetLock implements NumaLock {

    private final AtomicBoolean flag;

    public TestAndSetLock() {
        flag = new AtomicBoolean(false);
    }

    @Override
    public Object lock() {
        while (true) {
            if (flag.compareAndSet(false, true)) {
                return null;
            }
        }
    }

    @Override
    public void unlock(Object t) {
        flag.set(false);
    }
}
