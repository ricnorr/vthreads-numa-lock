package ru.ricnorr.numa.locks;

public interface NumaLock {
    Object lock(Object obj);

    void unlock(Object obj);

    boolean hasNext(Object obj);

    default boolean canUseNodeFromPreviousLocking() {
        return false;
    }

    default Object supplyNode() {
        throw new IllegalStateException("Not supported");
    }
}
