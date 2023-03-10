package ru.ricnorr.numa.locks;

import java.util.function.Supplier;

public abstract class AbstractNumaLock implements NumaLock {
    protected final Supplier<Integer> clusterIdSupplier;

    protected final ThreadLocal<Integer> clusterIdThreadLocal;

    protected final ThreadLocal<Integer> lockAcquiresThreadLocal;

    public AbstractNumaLock(Supplier<Integer> clusterIdSupplier) {
        this.clusterIdSupplier = clusterIdSupplier;
        this.clusterIdThreadLocal = ThreadLocal.withInitial(clusterIdSupplier);
        this.lockAcquiresThreadLocal = ThreadLocal.withInitial(() -> 0);
    }

    protected Integer getClusterId() {
        Thread carrierThread = Utils.getCurrentCarrierThread();
        int lockAcquires = Utils.getByThreadFromThreadLocal(clusterIdThreadLocal, carrierThread);
        int clusterId;
        if (lockAcquires >= 15000) {
            lockAcquires = 0;
            clusterId = clusterIdSupplier.get();
            Utils.setByThreadToThreadLocal(clusterIdThreadLocal, carrierThread, clusterId);
        } else {
            clusterId = Utils.getByThreadFromThreadLocal(clusterIdThreadLocal, carrierThread);
            lockAcquires += 1;
        }
        Utils.setByThreadToThreadLocal(lockAcquiresThreadLocal, carrierThread, lockAcquires);
        return clusterId;
    }

    @Override
    public abstract Object lock();

    @Override
    public abstract void unlock(Object obj);
}
