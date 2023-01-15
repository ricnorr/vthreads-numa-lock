package ru.ricnorr.numa.locks.atomics;

import kotlinx.atomicfu.AtomicInt;

import static kotlinx.atomicfu.AtomicFU.atomic;

public class KotlinxAtomicInt implements LockAtomicInt {
    final AtomicInt atomicInt;

    public KotlinxAtomicInt(int value) {
        this.atomicInt = atomic(value);
    }

    @Override
    public int get() {
        return atomicInt.getValue();
    }

    @Override
    public void set(int val) {
        atomicInt.setValue(val);
    }

    @Override
    public boolean cas(int expected, int newVal) {
        return atomicInt.compareAndSet(expected, newVal);
    }
}
