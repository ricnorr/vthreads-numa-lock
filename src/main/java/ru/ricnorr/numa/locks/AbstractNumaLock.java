package ru.ricnorr.numa.locks;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public abstract class AbstractNumaLock implements NumaLock {

    static final long MAX_WAIT = 100;

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
        return Utils.getByThreadFromThreadLocal(clusterIdThreadLocal, carrierThread);
    }
}
