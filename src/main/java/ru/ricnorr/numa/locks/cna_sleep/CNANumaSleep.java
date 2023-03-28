package ru.ricnorr.numa.locks.cna_sleep;

import ru.ricnorr.numa.locks.AbstractNumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;


public class CNANumaSleep extends AbstractNumaLock {

    CNALockCoreSleep cnaLockCoreSleep;

    Supplier<CNANodeSleep> cnaNodeFactory;


    public CNANumaSleep(boolean oversub, boolean yieldInEnd) {
        super(Utils::getNumaNodeId);
        this.cnaNodeFactory = CNANodeSleep::new;
        cnaLockCoreSleep = new CNALockCoreSleep(oversub, yieldInEnd);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object lock(Object obj) {
        int clusterId = getClusterId();
        CNANodeSleep node;
        if (obj != null) {
            node = (CNANodeSleep) obj;
            node.setSocketAtomically(clusterId);
        } else {
            node = cnaNodeFactory.get();
            node.setSocketAtomically(clusterId);
        }
        cnaLockCoreSleep.lock(node);
        return node;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unlock(Object t) {
        cnaLockCoreSleep.unlock((CNANodeSleep) t);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean hasNext(Object obj) {
        CNANodeSleep node = (CNANodeSleep) obj;
        return node.getNext() != null || node.getSpin() != CNALockCoreSleep.TRUE_VALUE;
    }

    @Override
    public boolean canUseNodeFromPreviousLocking() {
        return false;
    }

    @Override
    public Object supplyNode() {
        return cnaNodeFactory.get();
    }

    public static class CNALockCoreSleep {

        public static CNANodeSleep TRUE_VALUE = new CNANodeSleep();

        private final AtomicReference<CNANodeSleep> tail;

        private final boolean oversub;

        private final boolean yieldInEnd;

        public CNALockCoreSleep(boolean oversub, boolean yieldInEnd) {
            tail = new AtomicReference<>(null);
            this.oversub = oversub;
            this.yieldInEnd = yieldInEnd;
        }

        public void lock(CNANodeSleep me) {
            me.setNextAtomically(null);
            me.setSpinAtomically(null);
            me.setSecTailAtomically(null);
            if (oversub) {
                me.setShouldParkAtomically(false);
                me.setThreadAtomically(Thread.currentThread());
            }

            CNANodeSleep prevTail = tail.getAndSet(me);

            if (prevTail == null) {
                me.setSpinAtomically(TRUE_VALUE);
                return;
            }

            prevTail.setNextAtomically(me);
            while (me.getSpin() == null) {
                if (oversub && me.getShouldPark()) {
                    LockSupport.park();
                }
            }
        }

        public void unlock(CNANodeSleep me) {
            if (me.getNext() == null) {
                if (me.getSpin() == TRUE_VALUE) {
                    if (tail.compareAndSet(me, null)) {
                        if (yieldInEnd) {
                            Thread.yield();
                        }
                        return;
                    }
                } else { // у нас есть secondary queue
                    CNANodeSleep secHead = me.getSpin();
                    if (tail.compareAndSet(me, secHead.getSecTail())) {
                        secHead.setSpinAtomically(TRUE_VALUE);
                        // из sec очереди
                        if (oversub) {
                            LockSupport.unpark(secHead.getThread());
                        }
                        if (yieldInEnd) {
                            Thread.yield();
                        }
                        return;
                    }
                }

                /* Wait for successor to appear */
                while (me.getNext() == null) {
                    Thread.onSpinWait();
                }
            }
            CNANodeSleep succ = null;
            if (me.getSpin() == TRUE_VALUE) {
                succ = me.getNext();
                succ.setSpinAtomically(TRUE_VALUE);
                if (yieldInEnd) {
                    Thread.yield();
                }
                return;
            }
            if ((succ = find_successor(me)) != null) {
                succ.setSpinAtomically(me.getSpin());
            } else if (me.getSpin() != TRUE_VALUE) {
                succ = me.getSpin();
                succ.getSecTail().setNextAtomically(me.getNext());
                succ.setSecTailAtomically(null);
                succ.setSpinAtomically(TRUE_VALUE);
                if (oversub) {
                    LockSupport.unpark(succ.getThread());
                }
            } else {
                succ = me.getNext();
                succ.setSpinAtomically(TRUE_VALUE);
                if (oversub) {
                    LockSupport.unpark(succ.getThread());
                }
            }
            if (yieldInEnd) {
                Thread.yield();
            }
        }

        private CNANodeSleep find_successor(CNANodeSleep me) {
            CNANodeSleep next = me.getNext();
            int mySocket = me.getSocket();

            if (next.getSocket() == mySocket) {
                return next;
            }

            CNANodeSleep secHead = next;
            CNANodeSleep secTail = next;
            CNANodeSleep cur = next.getNext();

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
                } else {
                    if (oversub) {
                        cur.setShouldParkAtomically(true);
                    }
                }
                secTail = cur;
                cur = cur.getNext();
            }
            return null;
        }
    }
}
