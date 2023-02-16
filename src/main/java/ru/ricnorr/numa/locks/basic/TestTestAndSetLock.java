package ru.ricnorr.numa.locks.basic;

import ru.ricnorr.numa.locks.NumaLock;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestTestAndSetLock implements NumaLock {

    private final AtomicBoolean flag = new AtomicBoolean(false);

    @Override
    public Object lock() {
        while (true) {
            if (!flag.get() && flag.compareAndSet(false, true)) {
                return null;
            }
        }
    }

    @Override
    public void unlock(Object t) {
        flag.set(false);
    }

}

