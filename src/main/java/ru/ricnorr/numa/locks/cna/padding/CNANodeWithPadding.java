package ru.ricnorr.numa.locks.cna.padding;

import java.util.concurrent.atomic.AtomicReference;

public class CNANodeWithPadding {
    public long pad0, pad1, pad2, pad3, pad4, pad5, pad6, pad7, pad8, pad9, pad10, pad11;
    public final int socket;
    public final AtomicReference<CNANodeWithPadding> secTail;
    public volatile CNANodeWithPadding spin;
    public volatile CNANodeWithPadding next;


    public CNANodeWithPadding(int clusterID) {
        spin = null;
        socket = clusterID;
        secTail = new AtomicReference<>(null);
        next = null;
    }
}