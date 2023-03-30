package ru.ricnorr.numa.locks.basic;

import jdk.internal.vm.annotation.Contended;
import jdk.internal.vm.annotation.ReservedStackAccess;
import ru.ricnorr.numa.locks.NumaLock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.LockSupport;

public class CLH_SLEEP implements NumaLock {

//    NonfairSync sync = new NonfairSync();
//
//    @Override
//    public Object lock(Object obj) {
//        sync.lock();
//        return null;
//    }
//
//    @Override
//    public void unlock(Object obj) {
//        sync.release(1);
//    }
//
//    @Override
//    public boolean hasNext(Object obj) {
//        return false;
//    }

    private final QNodeCLHSleep head = new QNodeCLHSleep();

    private final AtomicReference<QNodeCLHSleep> tail = new AtomicReference<>(head);

    private final AtomicBoolean locked = new AtomicBoolean(false);

    private static class QNodeCLHSleep {
        @Contended
        private volatile boolean locked = false;

        @Contended
        private volatile Thread thread = null;

    }

    @Override
    public Object lock(Object obj) {
        if (locked.compareAndSet(false, true)) {
            head.locked = true;
            return null;
        }
        QNodeCLHSleep node;
        if (obj == null) {
            node = new QNodeCLHSleep();
        } else {
            node = (QNodeCLHSleep) obj;
        }
        node.locked = true;
        QNodeCLHSleep prev = tail.getAndSet(node);
        prev.thread = Thread.currentThread();
        int spins = 256;
        while (prev.locked) {
            spins--;
            if (spins == 0) {
                LockSupport.park();
                spins++;
            }
        }
        locked.set(true);
        prev.thread = null;
        return node;
    }

    @Override
    public void unlock(Object obj) {
        if (obj == null) {
            head.locked = false;
            LockSupport.unpark(head.thread);
        } else {
            QNodeCLHSleep qNode = (QNodeCLHSleep) obj;
            qNode.locked = false;
            LockSupport.unpark(qNode.thread);
        }
        locked.set(false);
    }

    @Override
    public boolean hasNext(Object obj) {
        return tail.get() != obj;
    }

}

abstract class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = -5179523762034025860L;


    /**
     * Checks for reentrancy and acquires if lock immediately
     * available under fair vs nonfair rules. Locking methods
     * perform initialTryLock check before relaying to
     * corresponding AQS acquire methods.
     */
    abstract boolean initialTryLock();

    @ReservedStackAccess
    final void lock() {
        if (!initialTryLock())
            acquire(1);
    }


    @ReservedStackAccess
    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        if (getExclusiveOwnerThread() != Thread.currentThread())
            throw new IllegalMonitorStateException();
        boolean free = (c == 0);
        if (free)
            setExclusiveOwnerThread(null);
        setState(c);
        return free;
    }

    protected final boolean isHeldExclusively() {
        // While we must in general read state before owner,
        // we don't need to do so to check if current thread is owner
        return getExclusiveOwnerThread() == Thread.currentThread();
    }

    /**
     * Reconstitutes the instance from a stream (that is, deserializes it).
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        setState(0); // reset to unlocked state
    }
}

/**
 * Sync object for non-fair locks
 */
final class NonfairSync extends Sync {

    private static final long serialVersionUID = 7316153563782823691L;

    boolean initialTryLock() {
        if (compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(Thread.currentThread());
        }
        return false;
    }

    /**
     * Acquire for non-reentrant cases after initialTryLock prescreen
     */
    protected boolean tryAcquire(int acquires) {
        if (getState() == 0 && compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(Thread.currentThread());
            return true;
        }
        return false;
    }
}
