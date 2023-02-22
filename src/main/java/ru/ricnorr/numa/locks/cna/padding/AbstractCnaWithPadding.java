package ru.ricnorr.numa.locks.cna.padding;

import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class AbstractCnaWithPadding implements NumaLock {
    ThreadLocal<CNANodeWithPadding> threadNodeThreadLocal;

    ThreadLocal<Integer> clusterIdThreadLocal;
    boolean useLightThread;

    CNALockCore cnaLockCore = new CNALockCore();

    public AbstractCnaWithPadding(boolean useLightThread, Supplier<Integer> clusterSupplier) {
        this.useLightThread = useLightThread;
        this.clusterIdThreadLocal = ThreadLocal.withInitial(clusterSupplier);
        threadNodeThreadLocal = ThreadLocal.withInitial(() -> new CNANodeWithPadding(clusterSupplier.get()));
    }

    @Override
    public Object lock() {
        if (useLightThread) {
            var node = new CNANodeWithPadding(Utils.getByThreadFromThreadLocal(clusterIdThreadLocal, Utils.getCurrentCarrierThread()));
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
            cnaLockCore.unlock(((CNANodeWithPadding) t));
        } else {
            cnaLockCore.unlock(threadNodeThreadLocal.get());
        }
    }

    public static class CNALockCore {

        public static CNANodeWithPadding TRUE_VALUE = new CNANodeWithPadding(-1);

        private final AtomicReference<CNANodeWithPadding> tail;

        public CNALockCore() {
            tail = new AtomicReference<>(null);
        }

        public void lock(CNANodeWithPadding me) {
            me.next = null;
            me.spin = null;
            me.secTail.set(null);
            CNANodeWithPadding prevTail = tail.getAndSet(me);
            if (prevTail == null) {
                me.spin = TRUE_VALUE;
                return;
            }

            prevTail.next = me;
            while (me.spin == null) {
                Thread.onSpinWait();
            }
        }

        public void unlock(CNANodeWithPadding me) {
            if (me.next == null) {
                if (me.spin == TRUE_VALUE) {
                    if (tail.compareAndSet(me, null)) {
                        return;
                    }
                } else { // у нас есть secondary queue
                    CNANodeWithPadding secHead = me.spin;
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

            CNANodeWithPadding succ = null;
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

        private CNANodeWithPadding find_successor(CNANodeWithPadding me) {
            CNANodeWithPadding next = me.next;
            int mySocket = me.socket;

            if (next.socket == mySocket) {
                return next;
            }

            CNANodeWithPadding secHead = next;
            CNANodeWithPadding secTail = next;
            CNANodeWithPadding cur = next.next;

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
