package ru.ricnorr.numa.locks.basic;

import ru.ricnorr.numa.locks.NumaLock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CLHLock implements NumaLock {
    private final ThreadLocal<QNodeCLH> prevNode = ThreadLocal.withInitial(() -> null);
    private final ThreadLocal<QNodeCLH> curNode = ThreadLocal.withInitial(QNodeCLH::new);
    private final AtomicReference<QNodeCLH> tail = new AtomicReference<>(new QNodeCLH());

    private static class QNodeCLH {
        private final AtomicBoolean locked = new AtomicBoolean(false);
    }

    @Override
    public Object lock(Object obj) {
        QNodeCLH node = curNode.get();
        node.locked.set(true);
        QNodeCLH prev = tail.getAndSet(node);
        prevNode.set(prev);
        while (prev.locked.get()) {
            Thread.onSpinWait();
        }
        return null;
    }

    @Override
    public void unlock(Object obj) {
        QNodeCLH qNode = curNode.get();
        qNode.locked.set(false);
        curNode.set(prevNode.get());
    }

    @Override
    public boolean hasNext(Object obj) {
        throw new IllegalStateException("Not implemented");
    }
}
