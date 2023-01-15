package ru.ricnorr.numa.locks.atomics;

import java.util.concurrent.atomic.AtomicReference;

public class JavaAtomicRef<T> implements LockAtomicRef<T> {
    AtomicReference<T> value;

    public JavaAtomicRef(T value) {
        this.value = new AtomicReference<>(value);
    }

    @Override
    public T get() {
        return value.get();
    }

    @Override
    public void set(T val) {
        value.set(val);
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
