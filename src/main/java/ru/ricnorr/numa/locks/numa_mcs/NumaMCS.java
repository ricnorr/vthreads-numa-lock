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

  ThreadLocal<Integer> numaNodeThreadLocal = ThreadLocal.withInitial(Utils::getNumaNodeId);

  ThreadLocal<Integer> lockAcquiresThreadLocal = ThreadLocal.withInitial(() -> 0);

  @Contended
  AtomicBoolean globalLock = new AtomicBoolean();

  public boolean runOnThisCarrierFeatureEnabled = false; // работает плохо на 4 потоках

  public boolean yieldIfDoesntChangedNuma = false; // работает плохо на 96 потоках

  public boolean yieldWhenWaitGlobal = false;
  // см NUMA_MCS_YIELD_WHEN_SPIN_ON_GLOBAL, сильно буста не дает, но это микробенчи

  public NumaMCS() {
    this.localQueues = new ArrayList<>();
    for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
      localQueues.add(new AtomicReference<>());
    }
  }

  private record NumaMCSRes(
      boolean fastPath,
      int numaId,

      Node node
  ) {
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
      }
      return new NumaMCSRes(false, numaId, node);
    }
    pred.next.set(node);
    while (node.spin) {
      LockSupport.park();
    }
    int iterations = 0;
    while (!globalLock.compareAndSet(false, true)) {
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
    System.out.println("Unlock " + Thread.currentThread().getName());
    var res = (NumaMCSRes) obj;
    if (res.fastPath) {
      globalLock.set(false);
      return;
    }
    var curNumaId = Utils.getByThreadFromThreadLocal(numaNodeThreadLocal, Utils.getCurrentCarrierThread());
    var localQueue = localQueues.get(res.numaId);
    if (localQueue.compareAndSet(res.node, null)) {
      globalLock.set(false);
      return;
    }
    while (res.node.next.get() == null) {
    }
    var nextNode = res.node.next.get();
    globalLock.set(false);
    nextNode.spin = false;

    if (curNumaId == res.numaId) {
      if (runOnThisCarrierFeatureEnabled) {
        LockSupport.unparkNextAndRunOnTheCarrier(nextNode.thread, Utils.getCurrentCarrierThread());
        return;
      } else if (yieldIfDoesntChangedNuma) {
        LockSupport.unpark(nextNode.thread);
        Thread.yield();
        return;
      }
    }
    LockSupport.unpark(nextNode.thread);
  }

  @Override
  public boolean hasNext(Object obj) {
    return false;
  }

  static class Node {

    @Contended
    Thread thread = Thread.currentThread();

    @Contended
    volatile boolean spin = true;

    @Contended
    AtomicReference<Node> next = new AtomicReference<>();

  }
}
