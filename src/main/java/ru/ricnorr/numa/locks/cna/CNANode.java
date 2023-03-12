package ru.ricnorr.numa.locks.cna;

import java.util.concurrent.atomic.AtomicReference;

public class CNANode implements CNANodeInterface {
    public volatile int socket;
    private final AtomicReference<CNANodeInterface> secTail;
    private volatile CNANodeInterface spin;
    private volatile CNANodeInterface next;


    public CNANode(int clusterID) {
        spin = null;
        socket = clusterID;
        secTail = new AtomicReference<>(null);
        next = null;
    }

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