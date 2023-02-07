package ru.ricnorr.numa.locks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
            qNode.status.set(LOCKED);
            QNode pred = hNode.tail.getAndSet(qNode);
            if (pred == null) {
                qNode.status.set(UNLOCKED);
            } else {
                pred.next.set(qNode);
                byte spins = 0, postSpins = 0;
                while (true) {
                    var status = qNode.status.get();
                    if (status != LOCKED && status != LOCKED_SLEEP) {
                        break;
                    }
                    if (overSubscription) {
                        if (spins != 0) {
                            spins--;
                            Thread.onSpinWait();
                        } else if (status == LOCKED) {
                            qNode.status.compareAndSet(LOCKED, LOCKED_SLEEP);
                        } else {
                            LockSupport.park(this);
                            spins = postSpins = (byte) ((postSpins << 1) | 1);
                            qNode.status.compareAndSet(LOCKED_SLEEP, LOCKED);
                        }
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
            qNode.status.set(WAIT);
            QNode pred = hNode.tail.getAndSet(qNode);
            if (pred != null) {
                pred.next.set(qNode);
                byte spins = 0, postSpins = 0;
                while (true) {
                    var status = qNode.status.get();
                    if (status != WAIT && status != WAIT_SLEEP) {
                        break;
                    }
                    if (overSubscription) {
                        if (spins != 0) {
                            spins--;
                            Thread.onSpinWait();
                        } else if (status == WAIT) {
                            qNode.status.compareAndSet(WAIT, WAIT_SLEEP);
                        } else {
                            LockSupport.park(this);
                            spins = postSpins = (byte) ((postSpins << 1) | 1);
                            qNode.status.compareAndSet(WAIT_SLEEP, WAIT);
                        }
                    } else {
                        Thread.onSpinWait();
                    }
                } // spin
                if (qNode.status.get() < ACQUIRE_PARENT) {
                    return;
                }
            }
            qNode.status.set(COHORT_START);
            lockH(hNode.node, hNode.parent);
        }
    }

    private void unlockH(HNode hNode, QNode qNode) {
        if (hNode.parent == null) { // top hierarchy
            releaseHelper(hNode, qNode, UNLOCKED);
            return;
        }
        int curCount = qNode.status.get();
        if (curCount == 10000) {
            unlockH(hNode.parent, hNode.node);
            releaseHelper(hNode, qNode, ACQUIRE_PARENT);
            return;
        }
        QNode succ = qNode.next.get();
        if (succ != null) {
            var prevStatus = succ.status.get();
            succ.status.set(curCount + 1);
            if (overSubscription && (prevStatus == LOCKED_SLEEP || prevStatus == WAIT_SLEEP)) {
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
            var prevStatus = succ.status.get();
            succ.status.set(val);
            if (overSubscription && (prevStatus == LOCKED_SLEEP || prevStatus == WAIT_SLEEP)) {
                LockSupport.unpark(succ.thread);
            }
        } else {
            if (l.tail.compareAndSet(i, null)) {
                return;
            }
            do {
                succ = i.next.get();
            } while (succ == null);
            var prevStatus = succ.status.get();
            succ.status.set(val);
            if (overSubscription && (prevStatus == LOCKED_SLEEP || prevStatus == WAIT_SLEEP)) {
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
        static int WAIT_SLEEP = Integer.MAX_VALUE;
        static int WAIT = Integer.MAX_VALUE - 1;
        static int ACQUIRE_PARENT = Integer.MAX_VALUE - 2;
        static int UNLOCKED = 0x0;
        static int LOCKED = 0x1;

        static int LOCKED_SLEEP = -1;

        static int COHORT_START = 0x1;

        private final AtomicReference<QNode> next = new AtomicReference<>(null);
        private AtomicInteger status = new AtomicInteger(WAIT);

        private volatile Thread thread = null;

    }
}
