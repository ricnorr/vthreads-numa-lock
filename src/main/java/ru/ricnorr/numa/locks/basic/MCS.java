package ru.ricnorr.numa.locks.basic;

import ru.ricnorr.numa.locks.NumaLock;

import java.util.concurrent.atomic.AtomicReference;

import static ru.ricnorr.numa.locks.Utils.spinWaitYield;

/**
 * MCS lock with active spin
 */
public class MCS implements NumaLock {

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
    public void unlock(Object object) {
        QNode headNode = node.get();
        if (headNode.next.get() == null) {
            if (tail.compareAndSet(headNode, null)) {
                return;
            }
            while (headNode.next.get() == null) {
                // WAIT when next чthread set headNode.next
            }
        }
        headNode.next.get().spin = false;
    }

    public static class QNode {
        private final AtomicReference<QNode> next = new AtomicReference<>(null);
        private volatile boolean spin = true;

    }
}
