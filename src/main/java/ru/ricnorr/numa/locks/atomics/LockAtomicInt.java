package ru.ricnorr.numa.locks.atomics;

public interface LockAtomicInt {
    int get();

    void set(int val);

    boolean cas(int expected, int newVal);
}
