package io.github.ricnorr.numa_locks.experimental;

import java.util.function.Supplier;

import io.github.ricnorr.numa_locks.LockUtils;
import io.github.ricnorr.numa_locks.VthreadNumaLock;

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
