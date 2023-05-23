package io.github.ricnorr.numa_locks.experimental;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import io.github.ricnorr.numa_locks.LockUtils;
import io.github.ricnorr.numa_locks.VthreadNumaLock;
import jdk.internal.vm.annotation.Contended;

@Contended
public class NumaMCSOneQueue implements VthreadNumaLock<NumaMCSOneQueue.UnlockInfo> {
  private static final VarHandle VALUE;

  static {
    try {
      MethodHandles.Lookup l = MethodHandles.lookup();
      VALUE = l.findVarHandle(io.github.ricnorr.numa_locks.NumaMCS.class, "globalLock", Boolean.TYPE);
    } catch (ReflectiveOperationException var1) {
      throw new ExceptionInInitializerError(var1);
    }
  }

  final List<AtomicReference<Node>> localQueues;
  public boolean tryAcquireFlag = true;
  ThreadLocal<Integer> numaNodeThreadLocal = ThreadLocal.withInitial(LockUtils::getNumaNodeId);
  ThreadLocal<Integer> lockAcquiresThreadLocal = ThreadLocal.withInitial(() -> 0);
  volatile boolean globalLock = false;

  public NumaMCSOneQueue() {
    this.localQueues = new ArrayList<>();
    for (int i = 0; i < LockUtils.NUMA_NODES_CNT; i++) {
      localQueues.add(new AtomicReference<>());
    }
  }


  private boolean casGlobalLock(boolean expected, boolean newValue) {
    return VALUE.compareAndSet(this, expected, newValue);
  }

  @Override
  public UnlockInfo lock() {
    var node = new Node();
    var numaId = 0;
//    var lockAcquires =
//        LockUtils.getByThreadFromThreadLocal(lockAcquiresThreadLocal, LockUtils.getCurrentCarrierThread());
//    lockAcquires++;
//    if (lockAcquires >= 10_000) {
//      lockAcquires = 1;
//      LockUtils.setByThreadToThreadLocal(numaNodeThreadLocal, LockUtils.getCurrentCarrierThread(),
//          LockUtils.getNumaNodeId());
//    }
//    LockUtils.setByThreadToThreadLocal(lockAcquiresThreadLocal, LockUtils.getCurrentCarrierThread(), lockAcquires);

    if (tryAcquireFlag) {
      if (casGlobalLock(false, true)) {
        return new UnlockInfo(true, numaId, node);
      }
    }
    var localQueue = localQueues.get(numaId);
    var pred = localQueue.getAndSet(node);
    if (pred == null) {
      while (!casGlobalLock(false, true)) {
        Thread.onSpinWait();
      }
      return new UnlockInfo(false, numaId, node);
    }
    pred.next = node;
    int iterations = 0;
    while (node.spin) {
      iterations++;
      if (iterations == 1024) {
        LockSupport.park();
        iterations = 0;
      }
    }
    while (!casGlobalLock(false, true)) {
      Thread.onSpinWait();
    }
    return new UnlockInfo(false, numaId, node);
  }

  @Override
  public void unlock(UnlockInfo unlockInfo) {
    globalLock = false;
    if (unlockInfo.fastPath) {
      return;
    }
    var localQueue = localQueues.get(unlockInfo.numaId);
    if (unlockInfo.node.next == null) {
      if (localQueue.compareAndSet(unlockInfo.node, null)) {
        return;
      }
      while (unlockInfo.node.next == null) {

      }
    }
    unlockInfo.node.next.spin = false;
    LockSupport.unpark(unlockInfo.node.next.thread);
  }

  public record UnlockInfo(
      boolean fastPath,
      int numaId,

      Node node
  ) {
  }

  @Contended
  private static class Node {

    Thread thread = Thread.currentThread();

    volatile boolean spin = true;

    volatile Node next = new Node();

  }
}
