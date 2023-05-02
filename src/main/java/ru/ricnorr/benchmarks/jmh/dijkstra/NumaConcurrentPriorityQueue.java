package ru.ricnorr.benchmarks.jmh.dijkstra;

import java.util.ArrayDeque;
import java.util.Queue;

import ru.ricnorr.numa.locks.NumaLock;

public class NumaConcurrentPriorityQueue<T> {
  final Queue<T> priorityQueue;

  final NumaLock lock;

  public NumaConcurrentPriorityQueue(NumaLock lock) {
    priorityQueue = new ArrayDeque<>();
    this.lock = lock;
  }

  boolean add(T x) {
    var obj = lock.lock(null);
    var res = priorityQueue.add(x);
    lock.unlock(obj);
    return res;
  }

  T poll() {
    var obj = lock.lock(null);
    var res = priorityQueue.poll();
    lock.unlock(obj);
    return res;
  }

}
