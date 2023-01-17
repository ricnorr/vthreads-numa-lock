package ru.ricnorr.numa.locks.atomics;

import kotlinx.atomicfu.AtomicRef;

import static kotlinx.atomicfu.AtomicFU.atomic;

public class KotlinxAtomicRef<T> implements LockAtomicRef<T> {
    private final AtomicRef<T> value;

    public KotlinxAtomicRef(T value) {
        this.value = atomic(value);
    }

    @Override
    public T get() {
        return value.getValue();
    }

    @Override
    public void set(T val) {
        value.setValue(val);
    }

    @Override
    public T getAndSet(T value) {
        return this.value.getAndSet(value);
    }

    @Override
    public boolean cas(T expected, T newVal) {
        return value.compareAndSet(expected, newVal);
    }
}
