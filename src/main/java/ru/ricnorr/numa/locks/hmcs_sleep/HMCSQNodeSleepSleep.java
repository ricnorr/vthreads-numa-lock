package ru.ricnorr.numa.locks.hmcs_sleep;

import jdk.internal.vm.annotation.Contended;

public class HMCSQNodeSleepSleep implements HMCSQNodeInterfaceSleep {

    @Contended("gr1")
    private volatile HMCSQNodeInterfaceSleep next = null;

    @Contended("gr1")
    private volatile int status = WAIT;

    @Contended("gr2")
    private volatile Thread thread;

    @Override
    public void setNextAtomically(HMCSQNodeInterfaceSleep hmcsQNode) {
        next = hmcsQNode;
    }

    @Override
    public HMCSQNodeInterfaceSleep getNext() {
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
