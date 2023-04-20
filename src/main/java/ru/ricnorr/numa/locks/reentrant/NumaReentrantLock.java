package ru.ricnorr.numa.locks.reentrant;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ru.ricnorr.numa.locks.NumaLock;

public class NumaReentrantLock implements NumaLock {

  private final Lock lock;

  public NumaReentrantLock(boolean fair) {
    this.lock = new ReentrantLock(fair);
  }

  @Override
  public Object lock(Object obj) {
    lock.lock();
    return null;
  }

  @Override
  public void unlock(Object obj) {
    lock.unlock();
  }

  @Override
  public boolean hasNext(Object obj) {
    throw new IllegalStateException("Not implemented error");
  }
}