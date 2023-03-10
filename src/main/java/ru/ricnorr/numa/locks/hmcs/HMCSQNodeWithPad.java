package ru.ricnorr.numa.locks.hmcs;

import jdk.internal.vm.annotation.Contended;

@Contended
public class HMCSQNodeWithPad implements HMCSQNodeInterface {
    private volatile HMCSQNodeInterface next = null;
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
