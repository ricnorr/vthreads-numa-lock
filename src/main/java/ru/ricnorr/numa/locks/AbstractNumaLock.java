package ru.ricnorr.numa.locks;

import java.util.function.Supplier;

public abstract class AbstractNumaLock implements NumaLock {

    static final long MAX_WAIT = 100;

    protected Supplier<Integer> clusterIdSupplier;

    protected final ThreadLocal<Integer> clusterIdThreadLocal;

    public AbstractNumaLock(Supplier<Integer> clusterIdSupplier) {
        this.clusterIdSupplier = clusterIdSupplier;
        this.clusterIdThreadLocal = ThreadLocal.withInitial(clusterIdSupplier);
    }

    protected Integer getClusterId() {
        Thread carrierThread = Utils.getCurrentCarrierThread();
        return Utils.getByThreadFromThreadLocal(clusterIdThreadLocal, carrierThread);
    }
}
