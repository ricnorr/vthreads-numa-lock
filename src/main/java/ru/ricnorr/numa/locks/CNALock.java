package ru.ricnorr.numa.locks;

import ru.ricnorr.numa.locks.atomics.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;


public class CNALock extends AbstractLock {


    ThreadLocal<Integer> socketID = ThreadLocal.withInitial(Utils::getClusterID);

    ThreadLocal<CNANode> node;

    ThreadLocal<Integer> lockAcquireCount = ThreadLocal.withInitial(() -> 0);


    CNALockCore cnaLockCore;

    CnaLockSpec cnaLockSpec;

    public CNALock(CnaLockSpec cnaLockSpec) {
        this.cnaLockSpec = cnaLockSpec;
        node = ThreadLocal.withInitial(() -> {
            CNANode node = new CNANode(cnaLockSpec, Thread.currentThread());
            node.socket.set(Utils.getClusterID());
            return node;
        });
        cnaLockCore = new CNALockCore();

    }

    @Override
    public void lock() {
        CNANode cnaNode = node.get();
        int lockAcquireNextValue = lockAcquireCount.get() + 1;
        if (lockAcquireNextValue == 1_000_000) {
            lockAcquireNextValue = 0;
            socketID.set(Utils.getClusterID());
            cnaNode.socket.set(socketID.get());
        }
        lockAcquireCount.set(lockAcquireNextValue);


        cnaLockCore.lock(node.get());
    }

    @Override
    public void unlock() {
        cnaLockCore.unlock(node.get(), socketID.get());
    }

    public class CNALockCore {

        private final LockAtomicRef<CNANode> tail;

        private final CNANode trueValue;

        public CNALockCore() {
            trueValue = new CNANode(cnaLockSpec, null);
            if (cnaLockSpec.useJavaAtomics) {
                tail = new JavaAtomicRef<>(null);
            } else {
                tail = new KotlinxAtomicRef<>(null);
            }
        }

        public long spinWait(long spinCounter, boolean useParkingOnSpin, long threshold) {
            if (spinCounter < threshold) {
                for (long i = 0; i < spinCounter; i++) {
                }
                spinCounter *= 2;
                return spinCounter;
            } else {
                if (useParkingOnSpin) {
                    LockSupport.park();
                } else {
                    Thread.yield();
                }
                return threshold;
            }
        }

        public void lock(CNANode me) {
            me.next.set(null);
            me.spin.set(null);
            me.secTail.set(null);

            CNANode prevTail = tail.getAndSet(me);

            if (prevTail == null) {
                me.spin.set(trueValue);
                return;
            }

            prevTail.next.set(me);
            long spinCount = 1;
            while (me.spin.get() == null) {
                spinCount = spinWait(spinCount, cnaLockSpec.useParkingOnSpin, cnaLockSpec.spinThreshold);
            }
        }

        public void unlock(CNANode me, int clusterID) {
            if (me.next.get() == null) {
                if (me.spin.get() == trueValue) {
                    if (tail.cas(me, null)) {
                        return;
                    }
                } else { // у нас есть secondary queue
                    CNANode secHead = me.spin.get();
                    if (tail.cas(me, secHead.secTail.get())) {
                        secHead.spin.set(trueValue);
                        if (cnaLockSpec.useParkingOnSpin) {
                            LockSupport.unpark(secHead.thread);
                        }
                        return;
                    }
                }

                /* Wait for successor to appear */
                long spinCounter = 1;
                while (me.next.get() == null) {
                    spinCounter = spinWait(spinCounter, false, cnaLockSpec.spinThreshold);
                }
            }
            CNANode succ = null;
            if (me.spin.get() == trueValue && (ThreadLocalRandom.current().nextInt() & 0xff) != 0) { // probability = 1 - (1 / 2**8) == 0.996
                succ = me.next.get();
                succ.spin.set(trueValue);
                if (cnaLockSpec.useParkingOnSpin) {
                    LockSupport.unpark(succ.thread);
                }
                return;
            }
            if (keep_lock_local() && (succ = find_successor(me, clusterID)) != null) {
                succ.spin.set(me.spin.get());
            } else if (me.spin.get() != trueValue) {
                succ = me.spin.get();
                succ.secTail.get().next.set(me.next.get());
                succ.secTail.set(null);
                succ.spin.set(trueValue);
            } else {
                succ = me.next.get();
                succ.spin.set(trueValue);
            }
            if (cnaLockSpec.useParkingOnSpin) {
                LockSupport.unpark(succ.thread);
            }
        }

        private CNANode find_successor(CNANode me, int socketID) {
            CNANode next = me.next.get();
            int mySocket = me.socket.get();

            if (next.socket.get() == mySocket) {
                return next;
            }

            CNANode secHead = next;
            CNANode secTail = next;
            CNANode cur = next.next.get();

            while (cur != null) {
                if (cur.socket.get() == mySocket) {
                    if (me.spin.get() != trueValue) {
                        me.spin.get().secTail.get().next.set(secHead);
                    } else {
                        me.spin.set(secHead);
                    }
                    //me.spin.getValue().next.set(null);
                    secTail.next.set(null);
                    me.spin.get().secTail.set(secTail);
                    return cur;
                }
                secTail = cur;
                cur = cur.next.get();
            }
            return null;
        }

        private boolean keep_lock_local() { // probability 0.9999
            return (ThreadLocalRandom.current().nextInt() & 0xffff) != 0;
        }
    }


    public static class CNANode {
        private final LockAtomicRef<CNANode> spin;// = atomic(null);
        private final LockAtomicInt socket;// = atomic(-1);
        private final LockAtomicRef<CNANode> secTail;// = atomic(null);
        private final LockAtomicRef<CNANode> next;// = atomic(null);


        private final Thread thread;

        public CNANode(CnaLockSpec spec, Thread thread) {
            if (spec.useJavaAtomics) {
                spin = new JavaAtomicRef<>(null);
                socket = new JavaAtomicInt(-1);
                secTail = new JavaAtomicRef<>(null);
                next = new JavaAtomicRef<>(null);
            } else {
                spin = new KotlinxAtomicRef<>(null);
                socket = new KotlinxAtomicInt(-1);
                secTail = new KotlinxAtomicRef<>(null);
                next = new KotlinxAtomicRef<>(null);
            }
            this.thread = thread;
        }
    }

}
