package ru.ricnorr.numa.locks;


import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class CNALock extends AbstractLock {


    ThreadLocal<Integer> socketID = ThreadLocal.withInitial(Utils::getClusterID);

    ThreadLocal<CNANode> node = ThreadLocal.withInitial(() -> {
        CNANode node = new CNANode(Thread.currentThread());
        node.socket.set(Utils.getClusterID());
        return node;
    });

    ThreadLocal<Integer> lockAcquireCount = ThreadLocal.withInitial(() -> 0);


    CNALockCore cnaLockCore = new CNALockCore();

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

    public static class CNALockCore {

        private final AtomicReference<CNANode> tail = new AtomicReference<>(null);

        private final CNANode trueValue = new CNANode(null);

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
            int spinCount = 1;
            while (me.spin.get() == null) {
                Thread.onSpinWait();
                //spinCount = spinWaitPark(spinCount);
            }
        }

        public void unlock(CNANode me, int clusterID) {
            if (me.next.get() == null) {
                if (me.spin.get() == trueValue) {
                    if (tail.compareAndSet(me, null)) {
                        return;
                    }
                } else { // у нас есть secondary queue
                    CNANode secHead = me.spin.get();
                    if (tail.compareAndSet(me, secHead.secTail.get())) {
                        secHead.spin.set(trueValue);
                        //LockSupport.unpark(secHead.thread);
                        return;
                    }
                }

                /* Wait for successor to appear */
                int spinCounter = 1;
                while (me.next.get() == null) {
                    Thread.onSpinWait();
                    //spinCounter = spinWaitYield(spinCounter);
                }
            }
            CNANode succ = null;
            if (me.spin.get() == trueValue && (ThreadLocalRandom.current().nextInt() & 0xff) != 0) { // probability = 1 - (1 / 2**8) == 0.996
                succ = me.next.get();
                succ.spin.set(trueValue);
                //LockSupport.unpark(succ.thread);
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
            //LockSupport.unpark(succ.thread);
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
        private final AtomicReference<CNANode> spin = new AtomicReference<>(null);
        private final AtomicInteger socket = new AtomicInteger(-1);
        private final AtomicReference<CNANode> secTail = new AtomicReference<>(null);
        private final AtomicReference<CNANode> next = new AtomicReference<>(null);

        private final Thread thread;

        public CNANode(Thread thread) {
            this.thread = thread;
        }
    }

}
