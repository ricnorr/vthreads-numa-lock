package ru.ricnorr.numa.locks;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;

import static ru.ricnorr.numa.locks.Utils.spinWaitYield;

public class MCS_WITH_PADDING extends AbstractLock {
    private final AtomicReference<QNode> tail = new AtomicReference<>(null);
    private final ThreadLocal<QNode> node = ThreadLocal.withInitial(QNode::new);

    @Override
    public void lock() {
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
    }

    @Override
    public void lockInterruptibly() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean tryLock() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean tryLock(long l, @NotNull TimeUnit timeUnit) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void unlock() {
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

    @NotNull
    @Override
    public Condition newCondition() {
        throw new RuntimeException("Not implemented");
    }

    public static class QNode {
        private final AtomicReference<QNode> next = new AtomicReference<>(null);

        private long pa1, pa2, pa3, pa4, pa5, pa6, pa7, pa8, pa9, pa10, pa11, pa12, pa13;
        private volatile boolean spin = true;

    }
}
