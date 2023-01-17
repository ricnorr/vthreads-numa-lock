package ru.ricnorr.numa.locks.atomics;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class JavaAtomicArray<T> implements LockAtomicArray<AtomicReference<T>> {
    List<AtomicReference<T>> list;

    @Override
    public AtomicReference<T> getByIndex(int ind) {
        return list.get(ind);
    }
}
