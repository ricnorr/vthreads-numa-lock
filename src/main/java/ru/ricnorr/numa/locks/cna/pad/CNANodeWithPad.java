package ru.ricnorr.numa.locks.cna.pad;

import jdk.internal.vm.annotation.Contended;
import ru.ricnorr.numa.locks.cna.CNANodeInterface;

import java.util.concurrent.atomic.AtomicReference;

@Contended
public class CNANodeWithPad implements CNANodeInterface {
    public final AtomicReference<CNANodeInterface> secTail = new AtomicReference<>(null);
    public volatile int socket = 0;
    public volatile CNANodeInterface spin = null;
    public volatile CNANodeInterface next = null;

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