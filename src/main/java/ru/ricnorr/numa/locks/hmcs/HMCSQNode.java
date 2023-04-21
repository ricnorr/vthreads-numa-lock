package ru.ricnorr.numa.locks.hmcs;

import jdk.internal.vm.annotation.Contended;

class HMCSQNode implements HMCSQNodeInterface {

  @Contended("gr1")
  private volatile HMCSQNodeInterface next = null;

  @Contended("gr1")
  private volatile int status = WAIT;

  @Override
  public void setNextAtomically(HMCSQNodeInterface hmcsQNode) {
    next = hmcsQNode;
  }

  @Override
  public HMCSQNodeInterface getNext() {
    return next;
  }

  @Override
  public void setStatusAtomically(int status) {
    this.status = status;
  }

  @Override
  public int getStatus() {
    return status;
  }
}
