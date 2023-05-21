package io.github.ricnorr.numa_locks.experimental;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.github.ricnorr.numa_locks.LockUtils;

import static io.github.ricnorr.numa_locks.experimental.HMCSQNode.ACQUIRE_PARENT;
import static io.github.ricnorr.numa_locks.experimental.HMCSQNode.COHORT_START;
import static io.github.ricnorr.numa_locks.experimental.HMCSQNode.LOCKED;
import static io.github.ricnorr.numa_locks.experimental.HMCSQNode.UNLOCKED;
import static io.github.ricnorr.numa_locks.experimental.HMCSQNode.WAIT;

public abstract class AbstractHMCS extends AbstractNumaLock<AbstractHMCS.InfoToUnlockHMCS> {


  protected final HNode[] leafs;

  private final ThreadLocal<HMCSQNode> threadLocalQNode;

  @SuppressWarnings("unchecked")
  public AbstractHMCS(Supplier<HMCSQNode> qNodeSupplier, Supplier<Integer> clusterIdSupplier, int leafsCnt) {
    super(clusterIdSupplier);
    this.leafs = (HNode[]) Array.newInstance(HNode.class, leafsCnt);
    this.threadLocalQNode = ThreadLocal.withInitial(qNodeSupplier);
  }

  @Override
  public InfoToUnlockHMCS lock() {
    HMCSQNode node = LockUtils.getByThreadFromThreadLocal(threadLocalQNode, LockUtils.getCurrentCarrierThread());
    int clusterId = getClusterId();
    lockH(node, leafs[clusterId]);
    return new InfoToUnlockHMCS(node, clusterId);
  }

  @Override
  public void unlock(InfoToUnlockHMCS infoToUnlock) {
    unlockH(leafs[infoToUnlock.clusterId], infoToUnlock.node);
  }

  private void lockH(HMCSQNode qNode, HNode hNode) {
    if (hNode.parent == null) {
      qNode.setNextAtomically(null);
      qNode.setStatusAtomically(LOCKED);
      HMCSQNode pred = hNode.tail.getAndSet(qNode);
      if (pred == null) {
        qNode.setStatusAtomically(UNLOCKED);
      } else {
        pred.setNextAtomically(qNode);
        while (qNode.getStatus() == LOCKED) {
          Thread.onSpinWait();
        } // spin
      }
    } else {
      qNode.setNextAtomically(null);
      qNode.setStatusAtomically(WAIT);
      HMCSQNode pred = hNode.tail.getAndSet(qNode);
      if (pred != null) {
        pred.setNextAtomically(qNode);
        while (qNode.getStatus() == WAIT) {
          Thread.onSpinWait();
        } // spin
        if (qNode.getStatus() < ACQUIRE_PARENT) {
          return;
        }
      }
      qNode.setStatusAtomically(COHORT_START);
      lockH(hNode.node, hNode.parent);
    }
  }

  private void unlockH(HNode hNode, HMCSQNode qNode) {
    if (hNode.parent == null) { // top hierarchy
      releaseHelper(hNode, qNode, UNLOCKED);
      return;
    }
    int curCount = qNode.getStatus();
    if (curCount == 100) {
      unlockH(hNode.parent, hNode.node);
      releaseHelper(hNode, qNode, ACQUIRE_PARENT);
      return;
    }
    HMCSQNode succ = qNode.getNext();
    if (succ != null) {
      succ.setStatusAtomically(curCount + 1);
      return;
    }
    unlockH(hNode.parent, hNode.node);
    releaseHelper(hNode, qNode, ACQUIRE_PARENT);
  }

  private void releaseHelper(HNode l, HMCSQNode i, int val) {
    HMCSQNode succ = i.getNext();
    if (succ != null) {
      succ.setStatusAtomically(val);
    } else {
      if (l.tail.compareAndSet(i, null)) {
        return;
      }
      do {
        succ = i.getNext();
      } while (succ == null);
      succ.setStatusAtomically(val);
    }
  }

  record InfoToUnlockHMCS(
      HMCSQNode node,
      int clusterId
  ) {
  }

  public static class HNode {
    private final AtomicReference<HMCSQNode> tail;
    private final HNode parent;
    HMCSQNode node;

    public HNode(HNode parent, HMCSQNode qNode) {
      this.parent = parent;
      this.tail = new AtomicReference<>(null);
      this.node = qNode;
    }
  }
}
