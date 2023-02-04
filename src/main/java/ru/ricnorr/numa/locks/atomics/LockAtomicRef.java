package ru.ricnorr.numa.locks.atomics;

public interface LockAtomicRef<T> {
    T get();

    void set(T value);

    T getAndSet(T value);

    boolean cas(T expected, T newVal);
}
