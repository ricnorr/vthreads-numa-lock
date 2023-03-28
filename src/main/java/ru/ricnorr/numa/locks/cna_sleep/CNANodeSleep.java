package ru.ricnorr.numa.locks.cna_sleep;

import java.util.concurrent.atomic.AtomicReference;

public class CNANodeSleep {
    private final AtomicReference<CNANodeSleep> secTail = new AtomicReference<>(null);
    public volatile int socket = 0;
    private volatile CNANodeSleep spin = null;
    private volatile CNANodeSleep next = null;

    private volatile Thread thread = null;

    private volatile boolean shouldPark = false;


    public void setSpinAtomically(CNANodeSleep cnaNode) {
        spin = cnaNode;
    }

    public void setNextAtomically(CNANodeSleep cnaNode) {
        next = cnaNode;
    }

    public void setSecTailAtomically(CNANodeSleep cnaNode) {
        secTail.set(cnaNode);
    }

    public void setSocketAtomically(int socketId) {
        this.socket = socketId;
    }

    public CNANodeSleep getSpin() {
        return spin;
    }

    public CNANodeSleep getNext() {
        return next;
    }

    public CNANodeSleep getSecTail() {
        return secTail.get();
    }

    public int getSocket() {
        return socket;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThreadAtomically(Thread thread) {
        this.thread = thread;
    }

    public boolean getShouldPark() {
        return shouldPark;
    }

    public void setShouldParkAtomically(boolean shouldPark) {
        this.shouldPark = shouldPark;
    }

}