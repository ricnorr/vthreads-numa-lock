package ru.ricnorr.numa.locks;

public interface NumaLock {
    Object lock(Object obj);

    void unlock(Object obj);

    boolean hasNext(Object obj);

}
