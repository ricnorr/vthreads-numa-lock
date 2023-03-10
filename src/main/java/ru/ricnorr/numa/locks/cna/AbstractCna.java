package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.AbstractNumaLock;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class AbstractCna<CnaNodeType extends CNANodeInterface> extends AbstractNumaLock {

    ThreadLocal<Integer> clusterIdThreadLocal;

    CNALockCore<CnaNodeType> cnaLockCore = new CNALockCore<>();

    Function<Integer, CnaNodeType> cnaNodeFactory;

    public AbstractCna(Supplier<Integer> clusterSupplier, Function<Integer, CnaNodeType> cnaNodeFactory) {
        super(clusterSupplier);
        this.clusterIdThreadLocal = ThreadLocal.withInitial(clusterSupplier);
        this.cnaNodeFactory = cnaNodeFactory;
    }

    @Override
    public Object lock() {
        int clusterId = getClusterId();
        var node = cnaNodeFactory.apply(clusterId);
        cnaLockCore.lock(node);
        return node;
    }

    @Override
    public void unlock(Object t) {
        cnaLockCore.unlock((CnaNodeType) t);
    }

    public boolean hasNext(CNANodeNoPad me) {
        return me.getNext() != null || me.getSpin() != CNALockCore.TRUE_VALUE;
    }

    public static class CNALockCore<T extends CNANodeInterface> {

        public static CNANodeInterface TRUE_VALUE = new CNANodeNoPad(-1);

        private final AtomicReference<CNANodeInterface> tail;

        public CNALockCore() {
            tail = new AtomicReference<>(null);
        }

        public void lock(T me) {
            me.setNextAtomically(null);
            me.setSpinAtomically(null); //me.spin = null;
            me.setSecTailAtomically(null);

            CNANodeInterface prevTail = tail.getAndSet(me);

            if (prevTail == null) {
                me.setSpinAtomically(TRUE_VALUE);
                return;
            }

            prevTail.setNextAtomically(me);
            while (me.getSpin() == null) {
                Thread.onSpinWait();
            }
        }

        public void unlock(T me) {
            if (me.getNext() == null) {
                if (me.getSpin() == TRUE_VALUE) {
                    if (tail.compareAndSet(me, null)) {
                        return;
                    }
                } else { // у нас есть secondary queue
                    CNANodeInterface secHead = me.getSpin();
                    if (tail.compareAndSet(me, secHead.getSecTail())) {
                        secHead.setSpinAtomically(TRUE_VALUE);
                        return;
                    }
                }

                /* Wait for successor to appear */
                while (me.getNext() == null) {
                    Thread.onSpinWait();
                }
            }
            CNANodeInterface succ = null;
            if (me.getSpin() == TRUE_VALUE) {
                succ = me.getNext();
                succ.setSpinAtomically(TRUE_VALUE);
                return;
            }
            if ((succ = find_successor(me)) != null) {
                succ.setSpinAtomically(me.getSpin());
            } else if (me.getSpin() != TRUE_VALUE) {
                succ = me.getSpin();
                succ.getSecTail().setNextAtomically(me.getNext());
                succ.setSecTailAtomically(null);
                succ.setSpinAtomically(TRUE_VALUE);
            } else {
                succ = me.getNext();
                succ.setSpinAtomically(TRUE_VALUE);
            }
        }

        private CNANodeInterface find_successor(CNANodeInterface me) {
            CNANodeInterface next = me.getNext();
            int mySocket = me.getSocket();

            if (next.getSocket() == mySocket) {
                return next;
            }

            CNANodeInterface secHead = next;
            CNANodeInterface secTail = next;
            CNANodeInterface cur = next.getNext();

            while (cur != null) {
                int curSocket = cur.getSocket();
                if (curSocket == mySocket) {
                    if (me.getSpin() != TRUE_VALUE) {
                        me.getSpin().getSecTail().setNextAtomically(secHead);
                    } else {
                        me.setSpinAtomically(secHead);
                    }
                    secTail.setNextAtomically(null);
                    me.getSpin().setSecTailAtomically(secTail);
                    return cur;
                }
                secTail = cur;
                cur = cur.getNext();
            }
            return null;
        }
    }
}
