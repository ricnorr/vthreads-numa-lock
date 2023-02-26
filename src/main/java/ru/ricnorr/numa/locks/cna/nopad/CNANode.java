package ru.ricnorr.numa.locks.cna.nopad;

import java.util.concurrent.atomic.AtomicReference;

public class CNANode {
    public final int socket;
    public final AtomicReference<CNANode> secTail;
    public volatile CNANode spin;
    public volatile CNANode next;


    public CNANode(int clusterID) {
        spin = null;
        socket = clusterID;
        secTail = new AtomicReference<>(null);
        next = null;
    }
}