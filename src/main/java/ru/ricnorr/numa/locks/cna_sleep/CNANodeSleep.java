package ru.ricnorr.numa.locks.cna_sleep;

import java.util.concurrent.atomic.AtomicReference;

public class CNANodeSleep {
  private final AtomicReference<CNANodeSleep> secTail = new AtomicReference<>(null);
  public volatile int socket = 0;
  public AtomicReference<CNANodeSleep> spin = new AtomicReference<>(null);
  private volatile CNANodeSleep next = null;
  private volatile Thread thread = null;

  public void setSpinAtomically(CNANodeSleep cnaNode) {
    spin.set(cnaNode);
  }

  public void setNextAtomically(CNANodeSleep cnaNode) {
    next = cnaNode;
  }

  public void setSecTailAtomically(CNANodeSleep cnaNode) {
    secTail.set(cnaNode);
  }

  public void setSocketAtomically(int socketId) {
    this.socket = socketId;
  }

  public CNANodeSleep getSpin() {
    return spin.get();
  }

  public CNANodeSleep getNext() {
    return next;
  }

  public CNANodeSleep getSecTail() {
    return secTail.get();
  }

  public int getSocket() {
    return socket;
  }

  public Thread getThread() {
    return thread;
  }

  public void setThreadAtomically(Thread thread) {
    this.thread = thread;
  }


}