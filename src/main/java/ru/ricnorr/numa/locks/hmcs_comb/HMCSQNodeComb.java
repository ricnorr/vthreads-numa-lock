package ru.ricnorr.numa.locks.hmcs_comb;

import jdk.internal.vm.annotation.Contended;

public class HMCSQNodeComb {

    int WAIT = Integer.MAX_VALUE;
    int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
    int UNLOCKED = 0x0;
    int LOCKED = 0x1;
    int COHORT_START = 0x1;

    @Contended("gr1")
    private volatile HMCSQNodeComb next = null;

    @Contended("gr1")
    private volatile int status = WAIT;

    public HMCSQNodeComb() {
    }

    public HMCSQNodeComb(HMCSQNodeComb next, int status) {
        this.next = next;
        this.status = status;
    }

    public void setNextAtomically(HMCSQNodeComb hmcsQNode) {
        next = hmcsQNode;
    }

    public HMCSQNodeComb getNext() {
        return next;
    }

    public void setStatusAtomically(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
