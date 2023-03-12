package ru.ricnorr.numa.locks.basic;

import jdk.internal.vm.annotation.Contended;
import ru.ricnorr.numa.locks.NumaLock;

import java.util.concurrent.atomic.AtomicReference;

/**
 * MCS lock with active spin
 */
public class MCS implements NumaLock {

    private final AtomicReference<QNode> tail = new AtomicReference<>(null);

    @Override
    public Object lock(Object obj) {

        QNode qnode;
        if (obj != null) {
            qnode = (QNode) obj;
        } else {
            qnode = new QNode();
        }
        qnode.spin = true;
        qnode.next.set(null);

        QNode pred = tail.getAndSet(qnode);
        if (pred != null) {
            pred.next.set(qnode);
            while (qnode.spin) {
                Thread.onSpinWait();
            }
        }
        return qnode;
    }


    @Override
    public void unlock(Object object) {
        QNode node = (QNode) object;
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return;
            }
            while (node.next.get() == null) {
                Thread.onSpinWait();
                // WAIT when next чthread set headNode.next
            }
        }
        node.next.get().spin = false;
    }

    @Override
    public boolean hasNext(Object obj) {
        QNode node = (QNode) obj;
        return node.next.get() != null;
    }

    @Contended
    public static class QNode {
        private final AtomicReference<QNode> next = new AtomicReference<>(null);
        private volatile boolean spin = true;

    }
}
