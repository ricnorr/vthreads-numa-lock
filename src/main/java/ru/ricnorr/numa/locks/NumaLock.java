package ru.ricnorr.numa.locks;

public interface NumaLock {
    Object lock();

    void unlock(Object obj);
}
