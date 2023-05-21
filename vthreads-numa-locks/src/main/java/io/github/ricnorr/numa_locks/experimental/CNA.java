package io.github.ricnorr.numa_locks.experimental;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CNA extends AbstractNumaLock<CNA.CNANode> {

  ThreadLocal<Integer> clusterIdThreadLocal;

  CNALockCore cnaLockCore = new CNALockCore();

  public CNA(Supplier<Integer> threadClusterSupplier) {
    super(threadClusterSupplier);
    this.clusterIdThreadLocal = ThreadLocal.withInitial(threadClusterSupplier);
  }

  @Override
  public CNANode lock() {
    int clusterId = getClusterId();
    CNANode node = new CNANode();
    node.socket = clusterId;
    cnaLockCore.lock(node);
    return node;
  }

  @Override
  public void unlock(CNANode node) {
    cnaLockCore.unlock(node);
  }

  public class CNALockCore {

    public CNANode TRUE_VALUE = new CNANode();

    private final AtomicReference<CNANode> tail;

    public CNALockCore() {
      tail = new AtomicReference<>(null);
    }

    public void lock(CNANode me) {
      me.next = null;
      me.spin = null;
      me.secTail.set(null);

      CNANode prevTail = tail.getAndSet(me);

      if (prevTail == null) {
        me.spin = TRUE_VALUE;
        return;
      }

      prevTail.next = me;
      while (me.spin == null) {
        Thread.onSpinWait();
      }
    }

    public void unlock(CNANode me) {
      if (me.next == null) {
        if (me.spin == TRUE_VALUE) {
          if (tail.compareAndSet(me, null)) {
            return;
          }
        } else { // у нас есть secondary queue
          CNANode secHead = me.spin;
          if (tail.compareAndSet(me, secHead.secTail.get())) {
            secHead.spin = TRUE_VALUE;
            return;
          }
        }

        /* Wait for successor to appear */
        while (me.next == null) {
          Thread.onSpinWait();
        }
      }
      CNANode succ = null;
//            if (me.getSpin() == TRUE_VALUE) {
//                succ = me.getNext();
//                succ.setSpinAtomically(TRUE_VALUE);
//                return;
//            }
      if ((succ = find_successor(me)) != null) {
        succ.spin = me.spin;
      } else if (me.spin != TRUE_VALUE) {
        succ = me.spin;
        succ.secTail.get().next = me.next;
        succ.secTail.set(null);
        succ.spin = TRUE_VALUE;
      } else {
        succ = me.next;
        succ.spin = TRUE_VALUE;
      }
    }

    private CNANode find_successor(CNANode me) {
      CNANode next = me.next;
      int mySocket = me.socket;

      if (next.socket == mySocket) {
        return next;
      }

      CNANode secHead = next;
      CNANode secTail = next;
      CNANode cur = next.next;

      while (cur != null) {
        int curSocket = cur.socket;
        if (curSocket == mySocket) {
          if (me.spin != TRUE_VALUE) {
            me.spin.secTail.get().next = secHead;
          } else {
            me.spin = secHead;
          }
          secTail.next = null;
          me.spin = secTail;
          return cur;
        }
        secTail = cur;
        cur = cur.next;
      }
      return null;
    }
  }

  public static class CNANode {
    private final AtomicReference<CNANode> secTail = new AtomicReference<>(null);
    public volatile int socket = 0;
    private volatile CNANode spin = null;
    private volatile CNANode next = null;

  }
}
