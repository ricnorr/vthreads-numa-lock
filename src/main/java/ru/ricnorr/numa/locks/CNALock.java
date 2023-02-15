package ru.ricnorr.numa.locks;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class CNALock extends AbstractLock {


    ThreadLocal<Integer> socketID = ThreadLocal.withInitial(Utils::getClusterID);

    ThreadLocal<CNANode> node;

    ThreadLocal<Integer> lockAcquireCount = ThreadLocal.withInitial(() -> 0);


    CNALockCore cnaLockCore;

    public CNALock() {
        node = ThreadLocal.withInitial(() -> {
            CNANode node = new CNANode();
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

    public static class CNANode {
        private final AtomicReference<CNANode> spin;
        private final AtomicInteger socket;
        private final AtomicReference<CNANode> secTail;
        private final AtomicReference<CNANode> next;


        public CNANode() {
            spin = new AtomicReference<>(null);
            socket = new AtomicInteger(-1);
            secTail = new AtomicReference<>(null);
            next = new AtomicReference<>(null);
        }
    }

    public class CNALockCore {

        private final AtomicReference<CNANode> tail;

        private final CNANode trueValue;

        public CNALockCore() {
            trueValue = new CNANode();
            tail = new AtomicReference<>(null);
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
            while (me.spin.get() == null) {
                Thread.onSpinWait();
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
                        return;
                    }
                }

                /* Wait for successor to appear */
                while (me.next.get() == null) {
                    Thread.onSpinWait();
                }
            }
            CNANode succ = null;
            //  && (ThreadLocalRandom.current().nextInt() & 0xff) != 0 // probability = 1 - (1 / 2**8) == 0.996
            if (me.spin.get() == trueValue) {
                succ = me.next.get();
                succ.spin.set(trueValue);
                return;
            }
            // keep_lock_local() &&
            if ((succ = find_successor(me, clusterID)) != null) {
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
                int curSocket = cur.socket.get();
                if (curSocket == mySocket) {
                    return successor_found(me, cur, secHead, secTail);
                }
                secTail = cur;
                cur = cur.next.get();
            }
            return null;
        }

        private CNANode successor_found(CNANode me, CNANode cur, CNANode secHead, CNANode secTail) {
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

        private boolean isFromOneBigNuma(int curSocket, int mySocket) {
            return (curSocket == 0 && mySocket == 1) || (curSocket == 1 && mySocket == 0)
                    || (curSocket == 2 && mySocket == 3) || (curSocket == 3 && mySocket == 2);
        }

        private boolean keep_lock_local() { // probability 0.9999
            return (ThreadLocalRandom.current().nextInt() & 0xffff) != 0;
        }
    }

}
