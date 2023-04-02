//package ru.ricnorr.numa.locks.cna_sleep;
//
//import java.util.concurrent.atomic.AtomicIntegerArray;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.concurrent.locks.LockSupport;
//import java.util.function.Supplier;
//
//import ru.ricnorr.numa.locks.AbstractNumaLock;
//import ru.ricnorr.numa.locks.Utils;
//
//
//public class CNANumaSleep extends AbstractNumaLock {
//
//  CNALockCoreSleep cnaLockCoreSleep;
//
//  Supplier<CNANodeSleep> cnaNodeFactory;
//
//  AtomicIntegerArray numaNodeActive = new AtomicIntegerArray(Utils.NUMA_NODES_CNT);
//
//
//  public CNANumaSleep() {
//    super(Utils::getNumaNodeId);
//    this.cnaNodeFactory = CNANodeSleep::new;
//    cnaLockCoreSleep = new CNALockCoreSleep();
//    for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
//      numaNodeActive.set(i, 0);
//    }
//  }
//
//  @Override
//  @SuppressWarnings("unchecked")
//  public Object lock(Object obj) {
//    int clusterId = getClusterId();
//    CNANodeSleep node;
//    if (obj != null) {
//      node = (CNANodeSleep) obj;
//      node.setSocketAtomically(clusterId);
//    } else {
//      node = cnaNodeFactory.get();
//      node.setSocketAtomically(clusterId);
//    }
//    cnaLockCoreSleep.lock(node);
//    if (numaNodeActive.get(clusterId) != 1) {
//      numaNodeActive.set(clusterId, 1);
//    }
//    return node;
//  }
//
//  @Override
//  @SuppressWarnings("unchecked")
//  public void unlock(Object t) {
//    cnaLockCoreSleep.unlock((CNANodeSleep) t);
//  }
//
//  @Override
//  @SuppressWarnings("unchecked")
//  public boolean hasNext(Object obj) {
//    CNANodeSleep node = (CNANodeSleep) obj;
//    return node.getNext() != null || node.getSpin() != CNALockCoreSleep.TRUE_VALUE;
//  }
//
//  @Override
//  public boolean canUseNodeFromPreviousLocking() {
//    return false;
//  }
//
//  @Override
//  public Object supplyNode() {
//    return cnaNodeFactory.get();
//  }
//
//  public class CNALockCoreSleep {
//
//    public static CNANodeSleep TRUE_VALUE = new CNANodeSleep();
//
//    public static CNANodeSleep NODE_WANT_SLEEP = new CNANodeSleep();
//
//    public static CNANodeSleep NODE_DO_NOT_SLEEP_PLS = new CNANodeSleep();
//
//    private final AtomicReference<CNANodeSleep> tail;
//
//    public CNALockCoreSleep() {
//      tail = new AtomicReference<>(null);
//    }
//
//    public void lock(CNANodeSleep me) {
//      me.setNextAtomically(null);
//      me.setSpinAtomically(null);
//      me.setSecTailAtomically(null);
//      me.setThreadAtomically(Thread.currentThread());
//      CNANodeSleep prevTail = tail.getAndSet(me);
//
//      if (prevTail == null) {
//        me.setSpinAtomically(TRUE_VALUE);
//        return;
//      }
//
//      prevTail.setNextAtomically(me);
//      while (me.getSpin() == null || me.getSpin() == NODE_DO_NOT_SLEEP_PLS) { // мы встали в очередь
//        var prevSocket = me.getSocket(); // с каким сокетом встали
//        if (numaNodeActive.get(prevSocket) == 0) {
//          me.setSocketAtomically(-1);
//          if (me.spin.compareAndSet(null, NODE_WANT_SLEEP)) {
//            LockSupport.park();
//            me.setSocketAtomically(
//                Utils.getByThreadFromThreadLocal(clusterIdThreadLocal, Utils.getCurrentCarrierThread()));
//            me.spin.set();
//          } else {
//            me.setSocketAtomically(prevSocket);
//          }
//        }
//      }
//    }
//
//    public void unlock(CNANodeSleep me) {
//      if (me.getNext() == null) {
//        if (me.getSpin() == TRUE_VALUE) {
//          if (tail.compareAndSet(me, null)) {
//            numaNodeActive.set(me.getSocket(), 0);
//            return;
//          }
//        } else { // у нас есть secondary queue
//          CNANodeSleep secHead = me.getSpin();
//          if (tail.compareAndSet(me, secHead.getSecTail())) {
//            secHead.setSpinAtomically(TRUE_VALUE);
//            LockSupport.unpark(secHead.getThread());
//            numaNodeActive.set(me.getSocket(), 0);
//            return;
//          }
//        }
//
//        /* Wait for successor to appear */
//        while (me.getNext() == null) {
//          Thread.onSpinWait();
//        }
//      }
//      CNANodeSleep succ = null;
//      if ((succ = find_successor(me)) != null) {
//        succ.setSpinAtomically(me.getSpin());
//      } else if (me.getSpin() != TRUE_VALUE) {
//        succ = me.getSpin();
//        succ.getSecTail().setNextAtomically(me.getNext());
//        succ.setSecTailAtomically(null);
//        succ.setSpinAtomically(TRUE_VALUE);
//        numaNodeActive.set(me.getSocket(), 0);
//        LockSupport.unpark(succ.getThread());
//      } else {
//        succ = me.getNext();
//        succ.setSpinAtomically(TRUE_VALUE);
//        numaNodeActive.set(me.getSocket(), 0);
//        LockSupport.unpark(succ.getThread());
//      }
//    }
//
//    private CNANodeSleep find_successor(CNANodeSleep me) {
//      CNANodeSleep next = me.getNext();
//      int mySocket = getClusterId();
//
//      if (next.getSocket() == mySocket) {
//        return next;
//      }
//
//      CNANodeSleep secHead = next;
//      CNANodeSleep secTail = next;
//      CNANodeSleep cur = next.getNext();
//
//      while (cur != null) {
//        int curSocket = cur.getSocket();
//        if (curSocket == mySocket && cur.spin.compareAndSet(null, NODE_DO_NOT_SLEEP_PLS)) {
//          if (me.getSpin() != TRUE_VALUE) {
//            me.getSpin().getSecTail().setNextAtomically(secHead);
//          } else {
//            me.setSpinAtomically(secHead);
//          }
//          secTail.setNextAtomically(null);
//          me.getSpin().setSecTailAtomically(secTail);
//          return cur;
//        } else {
//
//        }
//        secTail = cur;
//        cur = cur.getNext();
//      }
//      return null;
//    }
//  }
//}
