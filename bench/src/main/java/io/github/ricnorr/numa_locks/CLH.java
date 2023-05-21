package io.github.ricnorr.numa_locks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CLH implements VthreadNumaLock<CLH.QNodeCLH> {
  private final AtomicReference<QNodeCLH> tail = new AtomicReference<>(new QNodeCLH());

  public static class QNodeCLH {
    private final AtomicBoolean locked = new AtomicBoolean(false);
  }

  @Override
  public QNodeCLH lock() {
    QNodeCLH node = new QNodeCLH();
    node.locked.set(true);
    QNodeCLH prev = tail.getAndSet(node);
    while (prev.locked.get()) {
      Thread.onSpinWait();
    }
    return node;
  }

  @Override
  public void unlock(QNodeCLH node) {
    node.locked.set(false);
  }
}
