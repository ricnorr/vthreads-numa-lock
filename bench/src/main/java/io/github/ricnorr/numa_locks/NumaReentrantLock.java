package io.github.ricnorr.numa_locks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NumaReentrantLock implements VthreadNumaLock<Void> {

  private final Lock lock;

  public NumaReentrantLock(boolean fair) {
    this.lock = new ReentrantLock(fair);
  }

  @Override
  public Void lock() {
    lock.lock();
    return null;
  }

  @Override
  public void unlock(Void nothing) {
    lock.unlock();
  }
}