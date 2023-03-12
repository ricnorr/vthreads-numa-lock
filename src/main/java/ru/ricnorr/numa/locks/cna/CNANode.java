package ru.ricnorr.numa.locks.cna;

import java.util.concurrent.atomic.AtomicReference;

public class CNANode implements CNANodeInterface {
    public volatile int socket = 0;
    private final AtomicReference<CNANodeInterface> secTail = new AtomicReference<>(null);
    private volatile CNANodeInterface spin = null;
    private volatile CNANodeInterface next = null;
    

    @Override
    public void setSpinAtomically(CNANodeInterface cnaNode) {
        spin = cnaNode;
    }

    @Override
    public void setNextAtomically(CNANodeInterface cnaNode) {
        next = cnaNode;
    }

    @Override
    public void setSecTailAtomically(CNANodeInterface cnaNode) {
        secTail.set(cnaNode);
    }

    @Override
    public void setSocketAtomically(int socketId) {
        this.socket = socketId;
    }

    @Override
    public CNANodeInterface getSpin() {
        return spin;
    }

    @Override
    public CNANodeInterface getNext() {
        return next;
    }

    @Override
    public CNANodeInterface getSecTail() {
        return secTail.get();
    }

    @Override
    public int getSocket() {
        return socket;
    }
}