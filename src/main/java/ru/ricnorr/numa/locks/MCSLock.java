package ru.ricnorr.numa.locks;

import kotlinx.atomicfu.AtomicRef;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
        private final AtomicReference<QNode> next = new AtomicReference<>(null);
        private final AtomicBoolean spin = new AtomicBoolean(true);

        private final AtomicReference<Thread> thread = new AtomicReference<>(null);
    }

    private final AtomicReference<QNode> tail = new AtomicReference<>(null);
    private final AtomicReference<QNode> head = new AtomicReference<>(null);

    @Override
    public void lock() {
        QNode qnode = new QNode();
        QNode pred = tail.getAndSet(qnode);
        qnode.thread.set(Thread.currentThread());
        if (pred != null) {
            pred.next.set(qnode);
            while (qnode.spin.get()) {
                LockSupport.park(this);
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
            int spinCounter = 1;
            while (headNode.next.get() == null) {
                // WAIT when next thread set headNode.next
                spinCounter = spinWaitYield(spinCounter);
            }
        }
        headNode.next.get().spin.set(false);
        LockSupport.unpark(headNode.next.get().thread.get());
    }

    @NotNull
    @Override
    public Condition newCondition() {
        throw new RuntimeException("Not implemented");
    }
}
