package ru.ricnorr.numa.locks.hmcs_v;

import kotlin.Pair;
import ru.ricnorr.numa.locks.AbstractNumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static ru.ricnorr.numa.locks.hmcs.HMCSQNodeInterface.*;


public class HMCSNumaV extends AbstractNumaLock {

    private final HNode[] leafs;

    private final Supplier<HMCSQNodeVV> qNodeSupplier;

    private final boolean overSub;

    private final boolean yieldInEnd;

    private final int threshold;

    @SuppressWarnings("unchecked")
    public HMCSNumaV(boolean overSub, boolean yieldInEnd, int threshold) {
        super(Utils::getNumaNodeId);
        this.leafs = (HNode[]) Array.newInstance(HNode.class, Utils.NUMA_NODES_CNT);
        this.qNodeSupplier = HMCSQNodeVV::new;
        var root = new HNode(null, new HMCSQNodeVV());
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            leafs[i] = new HNode(root, new HMCSQNodeVV());
        }
        this.overSub = overSub;
        this.yieldInEnd = yieldInEnd;
        this.threshold = threshold;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object lock(Object obj) {
        HMCSQNodeVV node = qNodeSupplier.get();
        while (true) {
            int clusterId = getClusterId();
            if (overSub && leafs[clusterId].inQueue.get() > threshold) {
                Thread.yield();
            } else {
                lockH(node, leafs[clusterId], 0);
                return new Pair<>(node, clusterId);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unlock(Object obj) {
        Pair<HMCSQNodeVV, Integer> qnodeAndClusterId = (Pair<HMCSQNodeVV, Integer>) obj;
        unlockH(leafs[qnodeAndClusterId.component2()], qnodeAndClusterId.component1(), 0);
        if (overSub && yieldInEnd) {
            Thread.yield();
        }
    }


    @Override
    public boolean hasNext(Object obj) {
        throw new IllegalStateException("Not implemented");
    }

    private void lockH(HMCSQNodeVV qNode, HNode hNode, int lvl) {
        hNode.inQueue.incrementAndGet();
        if (hNode.parent == null) {
            qNode.setNextAtomically(null);
            qNode.setStatusAtomically(LOCKED);
            HMCSQNodeVInterface pred = hNode.tail.getAndSet(qNode);
            if (pred == null) {
                qNode.setStatusAtomically(UNLOCKED);
            } else {
                pred.setNextAtomically(qNode);
                while (qNode.getStatus() == LOCKED) {
                    Thread.onSpinWait();
                } // spin
            }
        } else {
            qNode.setNextAtomically(null);
            qNode.setStatusAtomically(WAIT);
            HMCSQNodeVInterface pred = hNode.tail.getAndSet(qNode);
            if (pred != null) {
                pred.setNextAtomically(qNode);
                while (qNode.getStatus() == WAIT) {
                    Thread.onSpinWait();
                } // spin
                if (qNode.getStatus() < ACQUIRE_PARENT) {
                    return;
                }
            }
            qNode.setStatusAtomically(COHORT_START);
            lockH(hNode.node, hNode.parent, lvl + 1);
        }
    }

    private void unlockH(HNode hNode, HMCSQNodeVInterface qNode, int lvl) {
        hNode.inQueue.decrementAndGet();
        if (hNode.parent == null) { // top hierarchy
            releaseHelper(hNode, qNode, UNLOCKED);
            return;
        }
        int curCount = qNode.getStatus();
        if (curCount == 10000) {
            unlockH(hNode.parent, hNode.node, lvl + 1);
            releaseHelper(hNode, qNode, ACQUIRE_PARENT);
            return;
        }
        HMCSQNodeVInterface succ = qNode.getNext();
        if (succ != null) {
            succ.setStatusAtomically(curCount + 1);
            return;
        }
        unlockH(hNode.parent, hNode.node, lvl + 1);
        releaseHelper(hNode, qNode, ACQUIRE_PARENT);
    }

    private void releaseHelper(HNode l, HMCSQNodeVInterface i, int val) {
        HMCSQNodeVInterface succ = i.getNext();
        if (succ != null) {
            succ.setStatusAtomically(val);
        } else {
            if (l.tail.compareAndSet(i, null)) {
                return;
            }
            do {
                succ = i.getNext();
            } while (succ == null);
            succ.setStatusAtomically(val);
        }
    }

    public class HNode {
        private final AtomicReference<HMCSQNodeVInterface> tail;
        private final HNode parent;

        private final AtomicInteger inQueue = new AtomicInteger(0);
        HMCSQNodeVV node;

        public HNode(HNode parent, HMCSQNodeVV qNode) {
            this.parent = parent;
            this.tail = new AtomicReference<>(null);
            this.node = qNode;
        }
    }
}

