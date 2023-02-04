package ru.ricnorr.numa.locks;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestAndSetLock extends AbstractLock {

    private final AtomicBoolean flag;

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
