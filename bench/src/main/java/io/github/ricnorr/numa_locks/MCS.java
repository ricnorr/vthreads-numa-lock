package io.github.ricnorr.numa_locks;

import java.util.concurrent.atomic.AtomicReference;

import jdk.internal.vm.annotation.Contended;


/**
 * MCS lock with active spin
 */
public class MCS implements VthreadNumaLock<MCS.QNode> {

  private final AtomicReference<QNode> tail = new AtomicReference<>(null);

  @Override
  public QNode lock() {

    QNode qnode = new QNode();
    qnode.spin = true;
    qnode.next.set(null);

    QNode pred = tail.getAndSet(qnode);
    if (pred != null) {
      pred.next.set(qnode);
      while (qnode.spin) {
        Thread.onSpinWait();
      }
    }
    return qnode;
  }


  @Override
  public void unlock(QNode node) {
    if (node.next.get() == null) {
      if (tail.compareAndSet(node, null)) {
        return;
      }
      while (node.next.get() == null) {
        Thread.onSpinWait();
      }
    }
    node.next.get().spin = false;
  }

  public static class QNode {

    @Contended
    private final AtomicReference<QNode> next = new AtomicReference<>(null);

    @Contended
    private volatile boolean spin = true;

  }
}
