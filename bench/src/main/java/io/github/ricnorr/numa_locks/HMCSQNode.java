package io.github.ricnorr.numa_locks;

import jdk.internal.vm.annotation.Contended;

class HMCSQNode {

  public static int WAIT = Integer.MAX_VALUE;
  public static int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
  public static int UNLOCKED = 0x0;
  public static int LOCKED = 0x1;
  public static int COHORT_START = 0x1;

  @Contended("gr1")
  private volatile HMCSQNode next = null;

  @Contended("gr1")
  private volatile int status = WAIT;

  @Contended("gr2")
  public volatile Thread thread = null;

  public void setNextAtomically(HMCSQNode hmcsQNode) {
    next = hmcsQNode;
  }

  public HMCSQNode getNext() {
    return next;
  }

  public void setStatusAtomically(int status) {
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
