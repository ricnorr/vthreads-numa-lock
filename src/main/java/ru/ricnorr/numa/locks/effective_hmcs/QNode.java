//package ru.ricnorr.numa.locks.effective_hmcs;
//
//import jdk.internal.vm.annotation.Contended;
//
//class QNode {
//
//  static int WAIT = Integer.MAX_VALUE;
//  static int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
//  static int UNLOCKED = 0x0;
//  static int LOCKED = 0x1;
//  static int COHORT_START = 0x1;
//
//  @Contended("gr1")
//  private volatile QNode next = null;
//
//  @Contended("gr1")
//  private volatile int status = WAIT;
//
//  @Contended("gr1")
//  volatile Thread thread = Thread.currentThread();
//
//  public void setNextAtomically(QNode hmcsQNode) {
//    next = hmcsQNode;
//  }
//
//  public QNode getNext() {
//    return next;
//  }
//
//  public void setStatusAtomically(int status) {
//    this.status = status;
//  }
//
//  public int getStatus() {
//    return status;
//  }
//}
