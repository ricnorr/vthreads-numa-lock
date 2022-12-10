package ru.ricnorr.numa.locks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CLHLock extends AbstractLock {
    private final ThreadLocal<QNodeCLH> prevNode = ThreadLocal.withInitial(() -> null);
    private final ThreadLocal<QNodeCLH> curNode = ThreadLocal.withInitial(QNodeCLH::new);
    private final AtomicReference<QNodeCLH> tail = new AtomicReference<>(new QNodeCLH());

    private static class QNodeCLH{
        private AtomicBoolean locked = new AtomicBoolean(false);
    }

    public void lock() {
        QNodeCLH node = curNode.get();
        node.locked.set(true);
        QNodeCLH prev = tail.getAndSet(node);
        prevNode.set(prev);
        while(prev.locked.get()) {
        }
    }

    public void unlock() {
        QNodeCLH qNode = curNode.get();
        qNode.locked.set(false);
        curNode.set(prevNode.get());
    }
}
