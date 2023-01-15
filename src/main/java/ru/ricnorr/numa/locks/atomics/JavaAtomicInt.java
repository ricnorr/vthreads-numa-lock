package ru.ricnorr.numa.locks.atomics;

import java.util.concurrent.atomic.AtomicInteger;

public class JavaAtomicInt implements LockAtomicInt {
    AtomicInteger atomicInteger;

    public JavaAtomicInt(int value) {
        this.atomicInteger = new AtomicInteger(value);
    }

    @Override
    public int get() {
        return atomicInteger.get();
    }

    @Override
    public void set(int val) {
        atomicInteger.set(val);
    }

    @Override
    public boolean cas(int expected, int newVal) {
        return atomicInteger.compareAndSet(expected, newVal);
    }
}
