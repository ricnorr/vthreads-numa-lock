package io.github.ricnorr.numa_locks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public class CNA extends AbstractNumaLock<CNA.CNANode> {

  ThreadLocal<Integer> clusterIdThreadLocal;

  CNALockCore cnaLockCore = new CNALockCore();

  AtomicBoolean flag = new AtomicBoolean(false);

  final boolean useFlag;

  public CNA(Supplier<Integer> threadClusterSupplier, boolean useFlag) {
    super(threadClusterSupplier);
    this.clusterIdThreadLocal = ThreadLocal.withInitial(threadClusterSupplier);
    this.useFlag = useFlag;
  }

  @Override
  public CNANode lock() {
    int clusterId = getClusterId();
    CNANode node = new CNANode();
    node.socket = clusterId;
    if (useFlag) {
      if (flag.compareAndSet(false, true)) {
        node.fastPath = true;
        return node;
      }
    }
    cnaLockCore.lock(node);
    if (useFlag) {
      while (!flag.compareAndSet(false, true)) {
      }
    }
    return node;
  }

  @Override
  public void unlock(CNANode node) {
    if (!node.fastPath) {
      cnaLockCore.unlock(node);
    }
    if (useFlag) {
      flag.set(false);
    }
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
        LockSupport.park();
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
            LockSupport.unpark(secHead.thread);
            return;
          }
        }

        /* Wait for successor to appear */
        while (me.next == null) {
          Thread.onSpinWait();
        }
      }
      CNANode succ = null;
      if ((succ = find_successor(me)) != null) {
        succ.spin = me.spin;
      } else if (me.spin != TRUE_VALUE) {
        succ = me.spin;
        succ.secTail.get().next = me.next;
        succ.spin = TRUE_VALUE;
      } else {
        succ = me.next;
        succ.spin = TRUE_VALUE;
      }
      LockSupport.unpark(succ.thread);
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
          me.spin.secTail.set(secTail);
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

    private final Thread thread = Thread.currentThread();

    public volatile int socket = -1;
    private volatile CNANode spin = null;
    private volatile CNANode next = null;

    private boolean fastPath = false;

  }
}
