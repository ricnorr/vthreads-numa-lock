package ru.ricnorr.numa.locks.hmcs;

import kotlin.Pair;
import ru.ricnorr.numa.locks.AbstractNumaLock;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static ru.ricnorr.numa.locks.hmcs.HMCSQNodeInterface.*;

public abstract class AbstractHMCS<QNode extends HMCSQNodeInterface> extends AbstractNumaLock {

    final HNode[] leafs;

    final Supplier<QNode> qNodeSupplier;

    @SuppressWarnings("unchecked")
    public AbstractHMCS(Supplier<QNode> qNodeSupplier, Supplier<Integer> clusterIdSupplier, int leafsCnt) {
        super(clusterIdSupplier);
        this.leafs = (HNode[]) Array.newInstance(HNode.class, leafsCnt);
        this.qNodeSupplier = qNodeSupplier;
    }

    @Override
    public Object lock(Object obj) {
        QNode node = qNodeSupplier.get();
        int clusterId = getClusterId();
        lockH(node, leafs[clusterId]);
        return new Pair<>(node, clusterId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unlock(Object obj) {
        Pair<QNode, Integer> qnodeAndClusterId = (Pair<QNode, Integer>) obj;
        unlockH(leafs[qnodeAndClusterId.component2()], qnodeAndClusterId.component1());
    }

    @Override
    public boolean hasNext(Object obj) {
        throw new IllegalStateException("Not implemented");
    }

    private void lockH(QNode qNode, HNode hNode) {
        if (hNode.parent == null) {
            qNode.setNextAtomically(null);
            qNode.setStatusAtomically(LOCKED);
            HMCSQNodeInterface pred = hNode.tail.getAndSet(qNode);
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
            HMCSQNodeInterface pred = hNode.tail.getAndSet(qNode);
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
            lockH(hNode.node, hNode.parent);
        }
    }

    private void unlockH(HNode hNode, HMCSQNodeInterface qNode) {
        if (hNode.parent == null) { // top hierarchy
            releaseHelper(hNode, qNode, UNLOCKED);
            return;
        }
        int curCount = qNode.getStatus();
        if (curCount == 10000) {
            unlockH(hNode.parent, hNode.node);
            releaseHelper(hNode, qNode, ACQUIRE_PARENT);
            return;
        }
        HMCSQNodeInterface succ = qNode.getNext();
        if (succ != null) {
            succ.setStatusAtomically(curCount + 1);
            return;
        }
        unlockH(hNode.parent, hNode.node);
        releaseHelper(hNode, qNode, ACQUIRE_PARENT);
    }

    private void releaseHelper(HNode l, HMCSQNodeInterface i, int val) {
        HMCSQNodeInterface succ = i.getNext();
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
        private final AtomicReference<HMCSQNodeInterface> tail;
        private final HNode parent;
        QNode node;

        public HNode(HNode parent, QNode qNode) {
            this.parent = parent;
            this.tail = new AtomicReference<>(null);
            this.node = qNode;
        }
    }
}
