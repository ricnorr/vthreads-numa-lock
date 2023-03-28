package ru.ricnorr.numa.locks.hmcs_v;

public interface HMCSQNodeVInterface {
    int WAIT = Integer.MAX_VALUE;
    int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
    int UNLOCKED = 0x0;
    int LOCKED = 0x1;
    int COHORT_START = 0x1;

    void setNextAtomically(HMCSQNodeVInterface hmcsQNode);

    HMCSQNodeVInterface getNext();

    void setStatusAtomically(int status);

    int getStatus();
}
