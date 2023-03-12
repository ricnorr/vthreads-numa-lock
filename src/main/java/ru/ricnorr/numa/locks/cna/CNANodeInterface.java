package ru.ricnorr.numa.locks.cna;

public interface CNANodeInterface {
    void setSpinAtomically(CNANodeInterface cnaNode);

    void setNextAtomically(CNANodeInterface cnaNode);

    void setSecTailAtomically(CNANodeInterface cnaNode);

    void setSocketAtomically(int socketId);

    CNANodeInterface getSpin();

    CNANodeInterface getNext();

    CNANodeInterface getSecTail();

    int getSocket();
}
