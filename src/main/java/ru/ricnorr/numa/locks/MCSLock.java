package ru.ricnorr.numa.locks;

import kotlinx.atomicfu.AtomicRef;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

import static kotlinx.atomicfu.AtomicFU.atomic;
import static ru.ricnorr.numa.locks.Utils.spinWaitYield;

/**
 * MCS lock with active spin
 */
public class MCSLock implements Lock {

    public static class QNode {
        private final AtomicRef<QNode> next = atomic(null);
        private final kotlinx.atomicfu.AtomicBoolean spin = atomic(true);

        private final AtomicRef<Thread> thread = atomic(null);
    }

    private final AtomicRef<QNode> tail = atomic(null);
    private final AtomicRef<QNode> head = atomic(null);

    @Override
    public void lock() {
        QNode qnode = new QNode();
        QNode pred = tail.getAndSet(qnode);
        qnode.thread.setValue(Thread.currentThread());
        if (pred != null) {
            pred.next.setValue(qnode);
            while (qnode.spin.getValue()) {
                LockSupport.park(this);
            }
        }
        head.setValue(qnode);
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
        QNode headNode = head.getValue();
        if (headNode.next.getValue() == null) {
            if (tail.compareAndSet(headNode, null)) {
                return;
            }
            int spinCounter = 1;
            while (headNode.next.getValue() == null) {
                // WAIT when next thread set headNode.next
                spinCounter = spinWaitYield(spinCounter);
            }
        }
        headNode.next.getValue().spin.setValue(false);
        LockSupport.unpark(headNode.next.getValue().thread.getValue());
    }

    @NotNull
    @Override
    public Condition newCondition() {
        throw new RuntimeException("Not implemented");
    }
}
