package ru.ricnorr.numa.locks.hmcs;

import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static ru.ricnorr.numa.locks.hmcs.AbstractHmcs.QNode.*;

public abstract class AbstractHmcs implements NumaLock {

    final List<HNode> leafs = new ArrayList<>();
    final ThreadLocal<QNode> localQNode = ThreadLocal.withInitial(QNode::new);
    final ThreadLocal<Integer> carrierClusterId;

    final boolean overSubscription;

    final boolean isLight;

    public AbstractHmcs(boolean overSubscription, boolean isLight, Supplier<Integer> carrierClusterIdSupplier) {
        if (isLight) {
            this.overSubscription = false;
        } else {
            this.overSubscription = overSubscription;
        }
        this.carrierClusterId = ThreadLocal.withInitial(carrierClusterIdSupplier);
        this.isLight = isLight;
    }

    @Override
    public Object lock() {
        if (isLight) {
            var node = new QNode();
            lockH(node, leafs.get(Utils.getByThreadFromThreadLocal(carrierClusterId, Utils.getCurrentCarrierThread())));
            return node;
        } else {
            lockH(localQNode.get(), leafs.get(carrierClusterId.get()));
            return null;
        }
    }
    
    @Override
    public void unlock(Object obj) {
        if (isLight) {
            unlockH(leafs.get(Utils.getByThreadFromThreadLocal(carrierClusterId, Utils.getCurrentCarrierThread())), (QNode) obj);
        } else {
            unlockH(leafs.get(carrierClusterId.get()), localQNode.get());
        }
    }

    private void lockH(QNode qNode, HNode hNode) {
        if (hNode.parent == null) {
            qNode.next = null;
            if (overSubscription) {
                qNode.thread = Thread.currentThread();
            }
            qNode.status = LOCKED;
            QNode pred = hNode.tail.getAndSet(qNode);
            if (pred == null) {
                qNode.status = UNLOCKED;
            } else {
                pred.next = qNode;
                while (qNode.status == LOCKED) {
                    if (overSubscription) {
                        LockSupport.park();
                    } else {
                        Thread.onSpinWait();
                    }
                } // spin
            }
        } else {
            qNode.next = null;
            if (overSubscription) {
                qNode.thread = Thread.currentThread();
            }
            qNode.status = WAIT;
            QNode pred = hNode.tail.getAndSet(qNode);
            if (pred != null) {
                pred.next = qNode;
                while (qNode.status == WAIT) {
                    if (overSubscription) {
                        LockSupport.park(this);
                    } else {
                        Thread.onSpinWait();
                    }
                } // spin
                if (qNode.status < ACQUIRE_PARENT) {
                    return;
                }
            }
            qNode.status = COHORT_START;
            lockH(hNode.node, hNode.parent);
        }
    }

    private void unlockH(HNode hNode, QNode qNode) {
        if (hNode.parent == null) { // top hierarchy
            releaseHelper(hNode, qNode, UNLOCKED);
            return;
        }
        int curCount = qNode.status;
        if (curCount == 10000) {
            unlockH(hNode.parent, hNode.node);
            releaseHelper(hNode, qNode, ACQUIRE_PARENT);
            return;
        }
        QNode succ = qNode.next;
        if (succ != null) {
            succ.status = curCount + 1;
            if (overSubscription) {
                LockSupport.unpark(succ.thread);
            }
            return;
        }
        unlockH(hNode.parent, hNode.node);
        releaseHelper(hNode, qNode, ACQUIRE_PARENT);
    }

    private void releaseHelper(HNode l, QNode i, int val) {
        QNode succ = i.next;
        if (succ != null) {
            succ.status = val;
            if (overSubscription) {
                LockSupport.unpark(succ.thread);
            }
        } else {
            if (l.tail.compareAndSet(i, null)) {
                return;
            }
            do {
                succ = i.next;
            } while (succ == null);
            succ.status = val;
            if (overSubscription) {
                LockSupport.unpark(succ.thread);
            }
        }
    }

    public static class HNode {
        private final AtomicReference<QNode> tail;
        QNode node;
        private final HNode parent;

        public HNode(HNode parent) {
            this.parent = parent;
            this.tail = new AtomicReference<>(null);
            this.node = new QNode();
        }
    }

    public static class QNode {
        static int WAIT = Integer.MAX_VALUE;
        static int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
        static int UNLOCKED = 0x0;
        static int LOCKED = 0x1;
        static int COHORT_START = 0x1;

        private volatile QNode next = null;
        private volatile int status = WAIT;

        private volatile Thread thread = null;

    }
}
