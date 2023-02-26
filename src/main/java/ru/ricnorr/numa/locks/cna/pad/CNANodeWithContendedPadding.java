package ru.ricnorr.numa.locks.cna.pad;

import jdk.internal.vm.annotation.Contended;

import java.util.concurrent.atomic.AtomicReference;

@Contended
public class CNANodeWithContendedPadding {
    public final int socket;
    public final AtomicReference<CNANodeWithContendedPadding> secTail;
    public volatile CNANodeWithContendedPadding spin;
    public volatile CNANodeWithContendedPadding next;


    public CNANodeWithContendedPadding(int clusterID) {
        spin = null;
        socket = clusterID;
        secTail = new AtomicReference<>(null);
        next = null;
    }
}