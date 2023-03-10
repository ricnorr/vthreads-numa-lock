package ru.ricnorr.numa.locks.hmcs;

public interface HMCSQNodeInterface {
    int WAIT = Integer.MAX_VALUE;
    int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
    int UNLOCKED = 0x0;
    int LOCKED = 0x1;
    int COHORT_START = 0x1;

    void setNextAtomically(HMCSQNodeInterface hmcsQNode);

    HMCSQNodeInterface getNext();

    void setStatusAtomically(int status);

    int getStatus();
}
