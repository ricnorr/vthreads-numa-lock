package ru.ricnorr.numa.locks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static ru.ricnorr.numa.locks.HMCS_ONLY_NUMA_HIERARCHY.QNode.*;


public class HMCS_ONLY_NUMA_HIERARCHY extends AbstractLock {
    final List<HNode> leafs = new ArrayList<>();
    final ThreadLocal<QNode> localQNode = ThreadLocal.withInitial(QNode::new);
    final ThreadLocal<Integer> localClusterID = ThreadLocal.withInitial(Utils::getClusterID);

    final boolean overSubscription;

    public HMCS_ONLY_NUMA_HIERARCHY(boolean overSubscription) {
        this.overSubscription = overSubscription;
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int numaNodesCount;
        if (availableProcessors == 96 || availableProcessors == 128) {
            numaNodesCount = 4;
        } else {
            numaNodesCount = 2;
        }
        var root = new HNode(null);
        for (int i = 0; i < numaNodesCount; i++) {
            leafs.add(new HNode(root));
        }
    }

    @Override
    public void lock() {
        lockH(localQNode.get(), leafs.get(localClusterID.get()));
    }

    @Override
    public void unlock() {
        unlockH(leafs.get(localClusterID.get()), localQNode.get());
    }

    private void lockH(QNode qNode, HNode hNode) {
        if (hNode.parent == null) {
            qNode.next.set(null);
            if (overSubscription) {
                qNode.thread = Thread.currentThread();
            }
            qNode.status = LOCKED;
            QNode pred = hNode.tail.getAndSet(qNode);
            if (pred == null) {
                qNode.status = UNLOCKED;
            } else {
                pred.next.set(qNode);
                while (qNode.status == LOCKED) {
                    if (overSubscription) {
                        LockSupport.park();
                    } else {
                        Thread.onSpinWait();
                    }
                } // spin
            }
        } else {
            qNode.next.set(null);
            if (overSubscription) {
                qNode.thread = Thread.currentThread();
            }
            qNode.status = WAIT;
            QNode pred = hNode.tail.getAndSet(qNode);
            if (pred != null) {
                pred.next.set(qNode);
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
        QNode succ = qNode.next.get();
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
        QNode succ = i.next.get();
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
                succ = i.next.get();
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
        private HNode parent;

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

        private final AtomicReference<QNode> next = new AtomicReference<>(null);
        private volatile int status = WAIT;

        private volatile Thread thread = null;

    }
}
