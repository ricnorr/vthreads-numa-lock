package io.github.ricnorr.numa_locks;

import java.util.concurrent.atomic.AtomicBoolean;

public class TTAS implements VthreadNumaLock<Void> {

  private final AtomicBoolean flag = new AtomicBoolean(false);

  @Override
  public Void lock() {
    while (true) {
      if (!flag.get() && flag.compareAndSet(false, true)) {
        return null;
      }
    }
  }

  @Override
  public void unlock(Void nothing) {
    flag.set(false);
  }
}

