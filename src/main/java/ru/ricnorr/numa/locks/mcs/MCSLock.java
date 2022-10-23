package ru.ricnorr.numa.locks.mcs;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * MCS lock with active spin
 */
public class MCSLock implements Lock {

    public static class QNode {
        public AtomicReference<QNode> next = new AtomicReference<>(null);
        public AtomicBoolean spin = new AtomicBoolean(true);
    }

    private AtomicReference<QNode> tail = new AtomicReference<>(null);
    private AtomicReference<QNode> head = new AtomicReference<>(null);

    @Override
    public void lock() {
        QNode qnode = new QNode();
        QNode pred = tail.getAndSet(qnode);
        if (pred != null) {
            pred.next.set(qnode);
            while (qnode.spin.get()) {
                // WAIT
            }
        }
        head.set(qnode);
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
        QNode headNode = head.get();
        if (headNode.next.get() == null) {
            if (tail.compareAndSet(headNode, null)) {
                return;
            }
            while (headNode.next.get() == null) {
                // WAIT when next thread set headNode.next
            }
        }
        headNode.next.get().spin.set(false);
    }

    @NotNull
    @Override
    public Condition newCondition() {
        throw new RuntimeException("Not implemented");
    }
}
