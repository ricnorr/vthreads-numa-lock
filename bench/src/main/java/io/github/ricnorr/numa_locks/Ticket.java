package io.github.ricnorr.numa_locks;

import java.util.concurrent.atomic.AtomicInteger;

public class Ticket implements VthreadNumaLock<Void> {

  private final AtomicInteger nowServing = new AtomicInteger(Integer.MIN_VALUE);
  private final AtomicInteger nextTicket = new AtomicInteger(Integer.MIN_VALUE);

  @Override
  public Void lock() {
    int myTicket = nextTicket.getAndIncrement();
    while (myTicket != nowServing.get()) {
      Thread.onSpinWait();
    }
    return null;
  }

  @Override
  public void unlock(Void nothing) {
    nowServing.getAndIncrement();
  }
}
