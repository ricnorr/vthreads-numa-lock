package ru.ricnorr.numa.locks.cna.nopadding;

import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class AbstractCna implements NumaLock {
    ThreadLocal<CNANode> threadNodeThreadLocal;

    ThreadLocal<Integer> clusterIdThreadLocal;
    boolean useLightThread;

    CNALockCore cnaLockCore = new CNALockCore();

    public AbstractCna(boolean useLightThread, Supplier<Integer> clusterSupplier) {
        this.useLightThread = useLightThread;
        this.clusterIdThreadLocal = ThreadLocal.withInitial(clusterSupplier);
        threadNodeThreadLocal = ThreadLocal.withInitial(() -> new CNANode(clusterSupplier.get()));
    }

    @Override
    public Object lock() {
        if (useLightThread) {
            var node = new CNANode(Utils.getByThreadFromThreadLocal(clusterIdThreadLocal, Utils.getCurrentCarrierThread()));
            cnaLockCore.lock(node);
            return node;
        } else {
            cnaLockCore.lock(threadNodeThreadLocal.get());
            return null;
        }
    }

    @Override
    public void unlock(Object t) {
        if (useLightThread) {
            cnaLockCore.unlock(((CNANode) t));
        } else {
            cnaLockCore.unlock(threadNodeThreadLocal.get());
        }
    }

    public static class CNALockCore {

        public static CNANode TRUE_VALUE = new CNANode(-1);

        private final AtomicReference<CNANode> tail;

        public CNALockCore() {
            tail = new AtomicReference<>(null);
        }

        public void lock(CNANode me) {
            me.next = null;
            me.spin = null;
            me.secTail.set(null);

            CNANode prevTail = tail.getAndSet(me);

            if (prevTail == null) {
                me.spin = TRUE_VALUE;
                return;
            }

            prevTail.next = me;
            while (me.spin == null) {
                Thread.onSpinWait();
            }
        }

        public void unlock(CNANode me) {
            if (me.next == null) {
                if (me.spin == TRUE_VALUE) {
                    if (tail.compareAndSet(me, null)) {
                        return;
                    }
                } else { // у нас есть secondary queue
                    CNANode secHead = me.spin;
                    if (tail.compareAndSet(me, secHead.secTail.get())) {
                        secHead.spin = TRUE_VALUE;
                        return;
                    }
                }

                /* Wait for successor to appear */
                while (me.next == null) {
                    Thread.onSpinWait();
                }
            }
            CNANode succ = null;
            if (me.spin == TRUE_VALUE) {
                succ = me.next;
                succ.spin = TRUE_VALUE;
                return;
            }
            if ((succ = find_successor(me)) != null) {
                succ.spin = me.spin;
            } else if (me.spin != TRUE_VALUE) {
                succ = me.spin;
                succ.secTail.get().next = me.next;
                succ.secTail.set(null);
                succ.spin = TRUE_VALUE;
            } else {
                succ = me.next;
                succ.spin = TRUE_VALUE;
            }
        }

        private CNANode find_successor(CNANode me) {
            CNANode next = me.next;
            int mySocket = me.socket;

            if (next.socket == mySocket) {
                return next;
            }

            CNANode secHead = next;
            CNANode secTail = next;
            CNANode cur = next.next;

            while (cur != null) {
                int curSocket = cur.socket;
                if (curSocket == mySocket) {
                    if (me.spin != TRUE_VALUE) {
                        me.spin.secTail.get().next = secHead;
                    } else {
                        me.spin = secHead;
                    }
                    secTail.next = null;
                    me.spin.secTail.set(secTail);
                    return cur;
                }
                secTail = cur;
                cur = cur.next;
            }
            return null;
        }
    }
}
