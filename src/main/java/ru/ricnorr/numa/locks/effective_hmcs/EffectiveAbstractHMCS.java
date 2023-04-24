//package ru.ricnorr.numa.locks.effective_hmcs;
//
//import java.lang.reflect.Array;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.concurrent.locks.LockSupport;
//
//import jdk.internal.vm.annotation.Contended;
//import ru.ricnorr.numa.locks.NumaLock;
//import ru.ricnorr.numa.locks.Utils;
//
//import static ru.ricnorr.numa.locks.effective_hmcs.QNode.ACQUIRE_PARENT;
//import static ru.ricnorr.numa.locks.effective_hmcs.QNode.COHORT_START;
//import static ru.ricnorr.numa.locks.effective_hmcs.QNode.LOCKED;
//import static ru.ricnorr.numa.locks.effective_hmcs.QNode.UNLOCKED;
//import static ru.ricnorr.numa.locks.effective_hmcs.QNode.WAIT;
//
//public abstract class EffectiveAbstractHMCS implements NumaLock {
//
//
//  protected HNode[] leafs;
//
//  protected HNode root;
//
//  ThreadLocal<Integer> cclIdThreadLocal = ThreadLocal.withInitial(Utils::getKunpengCCLId);
//
//  ThreadLocal<Integer> numaIdThreadLocal = ThreadLocal.withInitial(Utils::getNumaNodeId);
//
//  public EffectiveAbstractHMCS(int leafsCnt) {
//    this.leafs = (HNode[]) Array.newInstance(HNode.class, leafsCnt);
//  }
//
//  private record LockResult(
//      QNode node,
//      int cclId,
//      int numaId,
//      boolean fastPath
//  ) {
//  }
//
//  @Override
//  public Object lock(Object obj) {
//    QNode node = new QNode();
//    boolean fastPath = false;
//    int cclId = Utils.getByThreadFromThreadLocal(cclIdThreadLocal, Utils.getCurrentCarrierThread());
//    int numaId = Utils.getByThreadFromThreadLocal(numaIdThreadLocal, Utils.getCurrentCarrierThread());
//    if (root.lockIsTaken.compareAndSet(false, true)) {
//      fastPath = true;
//    } else {
//      lockH(node, leafs[cclId]);
//    }
//    return new LockResult(node, cclId, numaId, fastPath);
//  }
//
//  @Override
//  @SuppressWarnings("unchecked")
//  public void unlock(Object obj) {
//    var lockResult = (LockResult) obj;
//    if (lockResult.fastPath) { // fast path
//      root.lockIsTaken.set(false);
//      return;
//    }
//    unlockH(leafs[lockResult.cclId], lockResult.node, lockResult.numaId, lockResult.cclId);
//  }
//
//
//  @Override
//  public boolean hasNext(Object obj) {
//    throw new IllegalStateException("Not implemented");
//  }
//
//  private void lockH(QNode qNode, HNode hNode) {
//    if (hNode.parent == null) {
//      qNode.setNextAtomically(null);
//      qNode.setStatusAtomically(LOCKED);
//      qNode.thread = Thread.currentThread();
//      QNode pred = hNode.tail.getAndSet(qNode);
//      if (pred == null) {
//        qNode.setStatusAtomically(UNLOCKED);
//      } else {
//        pred.setNextAtomically(qNode);
//        int counter = 0;
//        while (qNode.getStatus() == LOCKED) {
//          counter++;
//          if (counter > 256) {
//            counter = 0;
//            LockSupport.park();
//          }
//        }
//      }
//      while (true) {
//        while (root.lockIsTaken.get()) {
//          // пока true
//        }
//        if (root.lockIsTaken.compareAndSet(false, true)) {
//          break;
//        }
//      }
//    } else {
//      qNode.setNextAtomically(null);
//      qNode.setStatusAtomically(WAIT);
//      qNode.thread = Thread.currentThread();
//      QNode pred = hNode.tail.getAndSet(qNode);
//      if (pred != null) {
//        pred.setNextAtomically(qNode);
//        int counter = 0;
//        while (qNode.getStatus() == WAIT) {
//          counter++;
//          if (counter > 256) {
//            counter = 0;
//            LockSupport.park();
//          }
//        } // spin
//        if (qNode.getStatus() < ACQUIRE_PARENT) {
//          while (true) {
//            while (root.lockIsTaken.get()) {
//            }
//            if (root.lockIsTaken.compareAndSet(false, true)) {
//              break;
//            }
//          }
//          return;
//        }
//      }
//      qNode.setStatusAtomically(COHORT_START);
//      lockH(hNode.node, hNode.parent);
//    }
//  }
//
//  private void unlockH(HNode hNode, QNode qNode, int numaNodeWhenLocking, int cclIdWhenLocking) {
//    if (hNode.parent == null) { // top hierarchy
//      root.lockIsTaken.set(false);
//      releaseHelper(hNode, qNode, UNLOCKED);
//      return;
//    }
//    int curCount = qNode.getStatus();
//    if (curCount == 1000) {
//      unlockH(hNode.parent, hNode.node, numaNodeWhenLocking, cclIdWhenLocking);
//      releaseHelper(hNode, qNode, ACQUIRE_PARENT);
//      return;
//    }
//    var currentNumaId = Utils.getByThreadFromThreadLocal(numaIdThreadLocal, Utils.getCurrentCarrierThread());
//    if (currentNumaId != numaNodeWhenLocking) {
//      unlockH(hNode.parent, hNode.node, numaNodeWhenLocking, cclIdWhenLocking);
//      releaseHelper(hNode, qNode, ACQUIRE_PARENT);
//      return;
//    }
//    QNode succ = qNode.getNext();
//    if (succ != null) {
//      root.lockIsTaken.set(false);
//      succ.setStatusAtomically(curCount + 1);
//      LockSupport.unparkNextAndRunOnTheCarrier(succ.thread, Utils.getCurrentCarrierThread());
//      return;
//    }
//    unlockH(hNode.parent, hNode.node, numaNodeWhenLocking, cclIdWhenLocking);
//    releaseHelper(hNode, qNode, ACQUIRE_PARENT);
//  }
//
//  private void releaseHelper(HNode l, QNode i, int val) {
//    QNode succ = i.getNext();
//    if (succ != null) {
//      succ.setStatusAtomically(val);
//      LockSupport.unpark(succ.thread);
//    } else {
//      if (l.tail.compareAndSet(i, null)) {
//        return;
//      }
//      do {
//        succ = i.getNext();
//      } while (succ == null);
//      succ.setStatusAtomically(val);
//      LockSupport.unpark(succ.thread);
//    }
//  }
//
//  @Contended
//  protected class HNode {
//
//    private final AtomicReference<QNode> tail;
//    private final HNode parent;
//    QNode node;
//
//    final AtomicBoolean lockIsTaken = new AtomicBoolean(false);
//
//    public HNode(HNode parent, QNode qNode) {
//      this.parent = parent;
//      this.tail = new AtomicReference<>(null);
//      this.node = qNode;
//    }
//  }
//}
