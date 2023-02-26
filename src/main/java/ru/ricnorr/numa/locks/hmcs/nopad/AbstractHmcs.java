package ru.ricnorr.numa.locks.hmcs.nopad;

import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static ru.ricnorr.numa.locks.hmcs.nopad.AbstractHmcs.QNode.*;

public abstract class AbstractHmcs implements NumaLock {

    public static final int CCL_SIZE = 4;
    final HNode[] leafs;
    final ThreadLocal<QNode> localQNode = ThreadLocal.withInitial(QNode::new);
    final ThreadLocal<Integer> carrierClusterId;

    final boolean overSubscription;

    final boolean isLight;

    public AbstractHmcs(boolean overSubscription, boolean isLight, Supplier<Integer> carrierClusterIdSupplier, int leafsCnt) {
        if (isLight) {
            this.overSubscription = false;
        } else {
            this.overSubscription = overSubscription;
        }
        this.leafs = new HNode[leafsCnt];
        this.carrierClusterId = ThreadLocal.withInitial(carrierClusterIdSupplier);
        this.isLight = isLight;
    }

    @Override
    public Object lock() {
        if (isLight) {
            var node = new QNode();
            lockH(node, leafs[Utils.getByThreadFromThreadLocal(carrierClusterId, Utils.getCurrentCarrierThread())]);
            return node;
        } else {
            lockH(localQNode.get(), leafs[carrierClusterId.get()]);
            return null;
        }
    }

    @Override
    public void unlock(Object obj) {
        if (isLight) {
            unlockH(leafs[Utils.getByThreadFromThreadLocal(carrierClusterId, Utils.getCurrentCarrierThread())], (QNode) obj);
        } else {
            unlockH(leafs[carrierClusterId.get()], localQNode.get());
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
        public static int WAIT = Integer.MAX_VALUE;
        public static int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
        public static int UNLOCKED = 0x0;
        public static int LOCKED = 0x1;
        public static int COHORT_START = 0x1;

        private volatile QNode next = null;
        private volatile int status = WAIT;

        private volatile Thread thread = null;

    }
}
