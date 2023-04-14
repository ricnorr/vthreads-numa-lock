package ru.ricnorr.numa.locks.hmcs_park;

public interface HMCSQNodeParkInterface {
  int WAIT = Integer.MAX_VALUE;
  int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
  int UNLOCKED = 0x0;
  int LOCKED = 0x1;
  int COHORT_START = 0x1;

  void setNextAtomically(HMCSQNodeParkInterface hmcsQNode);

  HMCSQNodeParkInterface getNext();

  void setStatusAtomically(int status);

  void setThreadAtomically(Thread thread);

  Thread getThread();

  int getStatus();
}
