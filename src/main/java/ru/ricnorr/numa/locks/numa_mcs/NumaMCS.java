package ru.ricnorr.numa.locks.numa_mcs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import jdk.internal.vm.annotation.Contended;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

public class NumaMCS implements NumaLock {

  final List<AtomicReference<Node>> localQueues;
  public boolean runOnThisCarrierFeatureEnabled = false; // работает плохо на 4 потоках
  public boolean yieldIfDoesntChangedNuma = false; // работает плохо на 96 потоках
  public boolean yieldWhenWaitGlobal = false;
  ThreadLocal<Integer> numaNodeThreadLocal = ThreadLocal.withInitial(Utils::getNumaNodeId);
  ThreadLocal<Integer> lockAcquiresThreadLocal = ThreadLocal.withInitial(() -> 0);

  AtomicBoolean globalLock = new AtomicBoolean();
  // см NUMA_MCS_YIELD_WHEN_SPIN_ON_GLOBAL, сильно буста не дает, но это микробенчи

  public NumaMCS() {
    this.localQueues = new ArrayList<>();
    for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
      localQueues.add(new AtomicReference<>());
    }
  }

  @Override
  public Object lock(Object obj) {
    var node = new Node();
    var numaId = Utils.getByThreadFromThreadLocal(numaNodeThreadLocal, Utils.getCurrentCarrierThread());
    var lockAcquires = Utils.getByThreadFromThreadLocal(lockAcquiresThreadLocal, Utils.getCurrentCarrierThread());
    lockAcquires++;
    if (lockAcquires >= 10_000) {
      lockAcquires = 1;
      Utils.setByThreadToThreadLocal(numaNodeThreadLocal, Utils.getCurrentCarrierThread(), Utils.getNumaNodeId());
    }
    Utils.setByThreadToThreadLocal(lockAcquiresThreadLocal, Utils.getCurrentCarrierThread(), lockAcquires);

    if (globalLock.compareAndSet(false, true)) {
      return new NumaMCSRes(true, numaId, node);
    }
    var localQueue = localQueues.get(numaId);
    var pred = localQueue.getAndSet(node);
    if (pred == null) {
      while (!globalLock.compareAndSet(false, true)) {
        Thread.onSpinWait();
      }
      return new NumaMCSRes(false, numaId, node);
    }
    pred.next.set(node);
    int iterations = 0;
    while (node.spin) {
      iterations++;
      if (iterations == 1024) {
        LockSupport.park();
        iterations = 0;
      }
    }
    iterations = 0;
    while (!globalLock.compareAndSet(false, true)) {
      Thread.onSpinWait();
      iterations++;
      if (yieldWhenWaitGlobal && iterations > 1024) {
        iterations = 0;
        Thread.yield();
      }
    }
    return new NumaMCSRes(false, numaId, node);
  }

  @Override
  public void unlock(Object obj) {
    var res = (NumaMCSRes) obj;
    globalLock.set(false);
    if (res.fastPath) {
      return;
    }
    var localQueue = localQueues.get(res.numaId);
    if (res.node.next.get() == null) {
      if (localQueue.compareAndSet(res.node, null)) {
        return;
      }
      while (res.node.next.get() == null) {

      }
    }
    res.node.next.get().spin = false;
    LockSupport.unpark(res.node.next.get().thread);
  }

  @Override
  public boolean hasNext(Object obj) {
    return false;
  }

  private record NumaMCSRes(
      boolean fastPath,
      int numaId,

      Node node
  ) {
  }

  @Contended
  static class Node {

    Thread thread = Thread.currentThread();

    volatile boolean spin = true;

    AtomicReference<Node> next = new AtomicReference<>();

  }
}
