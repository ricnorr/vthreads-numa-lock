package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.AbstractNumaLock;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class AbstractCNA<Node extends CNANodeInterface> extends AbstractNumaLock {

    ThreadLocal<Integer> clusterIdThreadLocal;

    CNALockCore<Node> cnaLockCore = new CNALockCore<>();

    Function<Integer, Node> cnaNodeFactory;

    public AbstractCNA(Supplier<Integer> clusterSupplier, Function<Integer, Node> cnaNodeFactory) {
        super(clusterSupplier);
        this.clusterIdThreadLocal = ThreadLocal.withInitial(clusterSupplier);
        this.cnaNodeFactory = cnaNodeFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object lock(Object obj) {
        int clusterId = getClusterId();
        Node node;
        if (obj != null) {
            node = (Node) obj;
            node.setSocketAtomically(clusterId);
        } else {
            node = cnaNodeFactory.apply(clusterId);
        }
        cnaLockCore.lock(node);
        return node;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unlock(Object t) {
        cnaLockCore.unlock((Node) t);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean hasNext(Object obj) {
        Node node = (Node) obj;
        return node.getNext() != null || node.getSpin() != CNALockCore.TRUE_VALUE;
    }

    public static class CNALockCore<Node extends CNANodeInterface> {

        public static CNANodeInterface TRUE_VALUE = new CNANode(-1);

        private final AtomicReference<CNANodeInterface> tail;

        public CNALockCore() {
            tail = new AtomicReference<>(null);
        }

        public void lock(Node me) {
            me.setNextAtomically(null);
            me.setSpinAtomically(null);
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

        public void unlock(Node me) {
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
