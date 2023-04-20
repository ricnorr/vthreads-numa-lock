package ru.ricnorr.numa.locks.hmcs_exp;

import jdk.internal.vm.annotation.Contended;

import static ru.ricnorr.numa.locks.hmcs.HMCSQNodeInterface.WAIT;

public class HMCSQNodeExp {

  @Contended("gr1")
  private volatile HMCSQNodeExp next = null;

  @Contended("gr1")
  private volatile int status = WAIT;

  @Contended("gr1")
  volatile Thread thread = Thread.currentThread();

  public void setNextAtomically(HMCSQNodeExp hmcsQNode) {
    next = hmcsQNode;
  }

  public HMCSQNodeExp getNext() {
    return next;
  }

  public void setStatusAtomically(int status) {
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
