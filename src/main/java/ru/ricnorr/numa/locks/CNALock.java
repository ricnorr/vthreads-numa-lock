package ru.ricnorr.numa.locks;

import kotlinx.atomicfu.AtomicInt;
import kotlinx.atomicfu.AtomicRef;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static kotlinx.atomicfu.AtomicFU.atomic;
import static ru.ricnorr.numa.locks.Utils.spinWait;

public class CNALock extends AbstractLock {


    ThreadLocal<Integer> socketID = ThreadLocal.withInitial(Utils::getClusterID);

    ThreadLocal<CNANode> node = ThreadLocal.withInitial(CNANode::new);
    CNALockCore cnaLockCore = new CNALockCore();

    @Override
    public void lock() {
        cnaLockCore.lock(node.get(), socketID.get());
    }

    @Override
    public void unlock() {
        cnaLockCore.unlock(node.get(), socketID.get());
    }

    public static class CNALockCore {

        private final AtomicReference<CNANode> tail = new AtomicReference<>(null);

        private final CNANode trueValue = new CNANode();

        public void lock(CNANode me, int socketID) {
            me.socket.setValue(-1);
            me.next.setValue(null);
            me.spin.setValue(null);
            me.secTail.setValue(null);
            me.thread.setValue(Thread.currentThread());

            CNANode prevTail = tail.getAndSet(me);

            if (prevTail == null) {
                me.spin.setValue(trueValue);
                return;
            }

            me.socket.setValue(socketID);
            prevTail.next.setValue(me);
            int spinCounter = 1;
            while (me.spin.getValue() == null) {
                LockSupport.park(this);
                //spinCounter = spinWait(spinCounter);
            }
        }

        public void unlock(CNANode me, int clusterID) {
            if (me.next.getValue() == null) {
                if (me.spin.getValue() == trueValue) {
                    if (tail.compareAndSet(me, null)) {
                        return;
                    }
                } else { // у нас есть secondary queue
                    CNANode secHead = me.spin.getValue();
                    if (tail.compareAndSet(me, secHead.secTail.getValue())) {
                        secHead.spin.setValue(trueValue);
                        return;
                    }
                }

                /* Wait for successor to appear */
                int spinCounter = 1;
                while (me.next.getValue() == null) {
                    spinCounter = spinWait(spinCounter);
                }
            }
            CNANode succ = null;
            if (keep_lock_local() && (succ = find_successor(me, clusterID)) != null) {
                succ.spin.setValue(me.spin.getValue());
            } else if (me.spin.getValue() != trueValue) {
                succ = me.spin.getValue();
                succ.secTail.getValue().next.setValue(me.next.getValue());
                succ.secTail.setValue(null);
                succ.spin.setValue(trueValue);
            } else {
                succ = me.next.getValue();
                succ.spin.setValue(trueValue);
            }
            LockSupport.unpark(succ.thread.getValue());
        }

        private CNANode find_successor(CNANode me, int socketID) {
            CNANode next = me.next.getValue();
            int mySocket = me.socket.getValue();

            if (next.socket.getValue() == mySocket) {
                return next;
            }

            CNANode secHead = next;
            CNANode secTail = next;
            CNANode cur = next.next.getValue();

            while (cur != null) {
                if (cur.socket.getValue() == mySocket) {
                    if (me.spin.getValue() != trueValue) {
                        me.spin.getValue().secTail.getValue().next.setValue(secHead);
                    } else {
                        me.spin.setValue(secHead);
                    }
                    //me.spin.getValue().next.set(null);
                    secTail.next.setValue(null);
                    me.spin.getValue().secTail.setValue(secTail);
                    return cur;
                }
                secTail = cur;
                cur = cur.next.getValue();
            }
            return null;
        }

        private boolean keep_lock_local() {
            return ThreadLocalRandom.current().nextInt(0, 500) % 499 != 0;
        }
    }


    public static class CNANode {
        private final AtomicRef<CNANode> spin = atomic(null);
        private final AtomicInt socket = atomic(-1);
        private final AtomicRef<CNANode> secTail = atomic(null);
        private final AtomicRef<CNANode> next = atomic(null);

        private final AtomicRef<Thread> thread = atomic(null);

    }

}
