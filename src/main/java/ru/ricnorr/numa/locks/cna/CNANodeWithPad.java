package ru.ricnorr.numa.locks.cna;

import jdk.internal.vm.annotation.Contended;

import java.util.concurrent.atomic.AtomicReference;

@Contended
public class CNANodeWithPad implements CNANodeInterface {
    public final int socket;
    public final AtomicReference<CNANodeInterface> secTail;
    public volatile CNANodeInterface spin;
    public volatile CNANodeInterface next;


    public CNANodeWithPad(int clusterID) {
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