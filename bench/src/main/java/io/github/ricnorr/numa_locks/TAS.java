package io.github.ricnorr.numa_locks;

import java.util.concurrent.atomic.AtomicBoolean;

public class TAS implements VthreadNumaLock<Void> {

  private final AtomicBoolean flag;

  public TAS() {
    flag = new AtomicBoolean(false);
  }

  @Override
  public Void lock() {
    while (true) {
      if (flag.compareAndSet(false, true)) {
        return null;
      }
    }
  }

  @Override
  public void unlock(Void nothing) {
    flag.set(false);
  }

}
