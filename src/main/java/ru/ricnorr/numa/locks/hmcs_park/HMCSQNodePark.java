package ru.ricnorr.numa.locks.hmcs_park;

import jdk.internal.vm.annotation.Contended;

public class HMCSQNodePark implements HMCSQNodeParkInterface {

  @Contended("gr1")
  private volatile HMCSQNodeParkInterface next = null;

  @Contended("gr1")
  private volatile int status = WAIT;

  @Contended("gr1")
  private volatile Thread thread = null;

  @Override
  public void setNextAtomically(HMCSQNodeParkInterface hmcsQNode) {
    next = hmcsQNode;
  }

  @Override
  public HMCSQNodeParkInterface getNext() {
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

  @Override
  public void setThreadAtomically(Thread thread) {
    this.thread = thread;
  }

  @Override
  public Thread getThread() {
    return thread;
  }
}
