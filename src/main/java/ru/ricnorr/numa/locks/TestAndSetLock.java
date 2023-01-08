package ru.ricnorr.numa.locks;

import java.util.concurrent.atomic.AtomicBoolean;

import static ru.ricnorr.numa.locks.Utils.spinWait;

public class TestAndSetLock extends AbstractLock {

    private AtomicBoolean flag;

    public TestAndSetLock() {
         flag = new AtomicBoolean(false);
    }

    @Override
    public void lock() {
        int spinCounter = 1;
        while (true) {
            spinCounter = spinWait(spinCounter);
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
