package ru.ricnorr.numa.locks.hmcs_sleep;

public interface HMCSQNodeInterfaceSleep {
    int WAIT = Integer.MAX_VALUE;
    int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
    int UNLOCKED = 0x0;
    int LOCKED = 0x1;
    int COHORT_START = 0x1;

    void setNextAtomically(HMCSQNodeInterfaceSleep hmcsQNode);

    HMCSQNodeInterfaceSleep getNext();

    void setStatusAtomically(int status);

    void setThreadAtomically(Thread thread);

    int getStatus();

    Thread getThread();
}
