package ru.ricnorr.numa.locks.atomics;

interface LockAtomicArray<T> {
    T getByIndex(int ind);
}
