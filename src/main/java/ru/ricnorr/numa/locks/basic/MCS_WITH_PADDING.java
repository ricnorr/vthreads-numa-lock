package ru.ricnorr.numa.locks.basic;

import ru.ricnorr.numa.locks.NumaLock;

import java.util.concurrent.atomic.AtomicReference;

import static ru.ricnorr.numa.locks.Utils.spinWaitYield;

public class MCS_WITH_PADDING implements NumaLock {
    private final AtomicReference<QNode> tail = new AtomicReference<>(null);
    private final ThreadLocal<QNode> node = ThreadLocal.withInitial(QNode::new);

    @Override
    public Object lock() {
        QNode qnode = node.get();
        qnode.spin = true;
        qnode.next.set(null);

        QNode pred = tail.getAndSet(qnode);
        if (pred != null) {
            pred.next.set(qnode);
            int spinCounter = 1;
            while (qnode.spin) {
                spinCounter = spinWaitYield(spinCounter);
            }
        }
        return null;
    }

    @Override
    public void unlock(Object obj) {
        QNode headNode = node.get();
        if (headNode.next.get() == null) {
            if (tail.compareAndSet(headNode, null)) {
                return;
            }
            while (headNode.next.get() == null) {
                Thread.onSpinWait();
                // WAIT when next чthread set headNode.next
            }
        }
        headNode.next.get().spin = false;
    }

    public static class QNode {
        private final AtomicReference<QNode> next = new AtomicReference<>(null);

        private long pa1, pa2, pa3, pa4, pa5, pa6, pa7, pa8, pa9, pa10, pa11, pa12, pa13;
        private volatile boolean spin = true;

    }
}
