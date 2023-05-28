package io.github.ricnorr.numa_locks;

import java.util.function.Supplier;

abstract class AbstractNumaLock<T> implements VthreadNumaLock<T> {

  protected Supplier<Integer> clusterIdSupplier;

  protected final ThreadLocal<Integer> clusterIdThreadLocal;

  public AbstractNumaLock(Supplier<Integer> clusterIdSupplier) {
    this.clusterIdSupplier = clusterIdSupplier;
    this.clusterIdThreadLocal = ThreadLocal.withInitial(clusterIdSupplier);
  }

  protected Integer getClusterId() {
    Thread carrierThread = LockUtils.getCurrentCarrierThread();
    return LockUtils.getByThreadFromThreadLocal(clusterIdThreadLocal, carrierThread);
  }
}
