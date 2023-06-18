package io.github.ricnorr.numa_locks;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static io.github.ricnorr.numa_locks.HMCSQNode.ACQUIRE_PARENT;
import static io.github.ricnorr.numa_locks.HMCSQNode.COHORT_START;
import static io.github.ricnorr.numa_locks.HMCSQNode.LOCKED;
import static io.github.ricnorr.numa_locks.HMCSQNode.UNLOCKED;
import static io.github.ricnorr.numa_locks.HMCSQNode.WAIT;

public abstract class AbstractHMCS extends AbstractNumaLock<AbstractHMCS.InfoToUnlockHMCS> {


  protected final HNode[] leafs;

  private final boolean useFlag;

  private final AtomicBoolean flag = new AtomicBoolean(false);

  @SuppressWarnings("unchecked")
  public AbstractHMCS(Supplier<HMCSQNode> qNodeSupplier, Supplier<Integer> clusterIdSupplier, int leafsCnt,
                      boolean useFlag) {
    super(clusterIdSupplier);
    this.leafs = (HNode[]) Array.newInstance(HNode.class, leafsCnt);
    this.useFlag = useFlag;
  }

  @Override
  public InfoToUnlockHMCS lock() {
    HMCSQNode node = new HMCSQNode();
    int clusterId = getClusterId();
    if (useFlag) {
      if (flag.compareAndSet(false, true)) {
        return new InfoToUnlockHMCS(node, clusterId, true);
      }
    }
    lockH(node, leafs[clusterId]);
    if (useFlag) {
      while (!flag.compareAndSet(false, true)) {
      }
    }
    return new InfoToUnlockHMCS(node, clusterId, false);
  }

  @Override
  public void unlock(InfoToUnlockHMCS infoToUnlock) {
    if (!infoToUnlock.fastPath) {
      unlockH(leafs[infoToUnlock.clusterId], infoToUnlock.node);
    }
    if (useFlag) {
      flag.set(false);
    }
  }

  private void lockH(HMCSQNode qNode, HNode hNode) {
    if (hNode.parent == null) {
      qNode.setNextAtomically(null);
      qNode.setStatusAtomically(LOCKED);
      qNode.thread = Thread.currentThread();
      HMCSQNode pred = hNode.tail.getAndSet(qNode);
      if (pred == null) {
        qNode.setStatusAtomically(UNLOCKED);
      } else {
        pred.setNextAtomically(qNode);
        while (qNode.getStatus() == LOCKED) {
          LockSupport.park();
        } // spin
      }
    } else {
      qNode.setNextAtomically(null);
      qNode.setStatusAtomically(WAIT);
      qNode.thread = Thread.currentThread();
      HMCSQNode pred = hNode.tail.getAndSet(qNode);
      if (pred != null) {
        pred.setNextAtomically(qNode);
        while (qNode.getStatus() == WAIT) {
          LockSupport.park();
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
      LockSupport.unpark(succ.thread);
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
    LockSupport.unpark(succ.thread);
  }

  record InfoToUnlockHMCS(
      HMCSQNode node,
      int clusterId,

      boolean fastPath
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
