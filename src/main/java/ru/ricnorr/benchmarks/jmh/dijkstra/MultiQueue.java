package ru.ricnorr.benchmarks.jmh.dijkstra;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

public class MultiQueue {

  private final int n;

  private final List<Queue<JmhDijkstraBenchmark.NodeIdAndDistance>> queues;

  private final List<NumaLock> numaLocks;

  private final Random random = new Random();

  private final AtomicInteger activeCounter = new AtomicInteger(0);

  public MultiQueue(int n, LockType lockType) {
    this.n = n;
    queues = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      queues.add(new ArrayDeque<>());
    }
    numaLocks = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      numaLocks.add(Utils.initLock(lockType, 0));
    }
  }

  public void decCounter() {
    activeCounter.decrementAndGet();
  }


  public int valueCounter() {
    return activeCounter.get();
  }

  public JmhDijkstraBenchmark.NodeIdAndDistance getElement() {
    var i = 0;
    var j = 0;
    do {
      i = random.nextInt(0, n);
      j = random.nextInt(0, n);
    } while (i == j);
    var mn = Math.min(i, j);
    var mx = Math.max(i, j);
    var minObj = numaLocks.get(mn).lock(null);
    var maxObj = numaLocks.get(mx).lock(null);
    var fQueue = queues.get(mn);
    var sQueue = queues.get(mx);
    if (fQueue.isEmpty() && !sQueue.isEmpty()) {
      var res = sQueue.poll();
      numaLocks.get(mx).unlock(maxObj);
      numaLocks.get(mn).unlock(minObj);
      return res;
    }
    if (fQueue.isEmpty() && sQueue.isEmpty()) {
      numaLocks.get(mx).unlock(maxObj);
      numaLocks.get(mn).unlock(minObj);
      return null;
    }
    if (sQueue.isEmpty() && !fQueue.isEmpty()) {
      var res = fQueue.poll();
      numaLocks.get(mx).unlock(maxObj);
      numaLocks.get(mn).unlock(minObj);
      return res;
    }
    var fElement = fQueue.peek();
    var sElement = sQueue.peek();
    JmhDijkstraBenchmark.NodeIdAndDistance res;
    if (fElement.distance() < sElement.distance()) {
      fQueue.poll();
      res = fElement;
    } else {
      sQueue.poll();
      res = sElement;
    }
    numaLocks.get(mx).unlock(maxObj);
    numaLocks.get(mn).unlock(minObj);
    return res;
  }

  public void addElement(JmhDijkstraBenchmark.NodeIdAndDistance node) {
    var i = random.nextInt(0, n);
    activeCounter.incrementAndGet();
    var obj1 = numaLocks.get(i).lock(null);
    queues.get(i).add(node);
    numaLocks.get(i).unlock(obj1);
  }
}
