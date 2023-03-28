package ru.ricnorr.numa.locks.hmcs_v;

import jdk.internal.vm.annotation.Contended;

public class HMCSQNodeVV implements HMCSQNodeVInterface {

    @Contended("gr1")
    private volatile HMCSQNodeVInterface next = null;

    @Contended("gr1")
    private volatile int status = WAIT;

    @Override
    public void setNextAtomically(HMCSQNodeVInterface hmcsQNode) {
        next = hmcsQNode;
    }

    @Override
    public HMCSQNodeVInterface getNext() {
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
