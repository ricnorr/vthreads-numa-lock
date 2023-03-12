package ru.ricnorr.numa.locks.basic;

import ru.ricnorr.numa.locks.NumaLock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CLH implements NumaLock {
    private final AtomicReference<QNodeCLH> tail = new AtomicReference<>(new QNodeCLH());

    private static class QNodeCLH {
        private final AtomicBoolean locked = new AtomicBoolean(false);
    }

    @Override
    public Object lock(Object obj) {
        QNodeCLH node;
        if (obj == null) {
            node = new QNodeCLH();
        } else {
            node = (QNodeCLH) obj;
        }
        node.locked.set(true);
        QNodeCLH prev = tail.getAndSet(node);
        while (prev.locked.get()) {
            Thread.onSpinWait();
        }
        return node;
    }

    @Override
    public void unlock(Object obj) {
        QNodeCLH qNode = (QNodeCLH) obj;
        qNode.locked.set(false);
    }

    @Override
    public boolean hasNext(Object obj) {
        return tail.get() != obj;
    }
}
