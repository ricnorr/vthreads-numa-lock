package ru.ricnorr.numa.locks.hmcs_sleep;

import kotlin.Pair;
import ru.ricnorr.numa.locks.AbstractNumaLock;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import static ru.ricnorr.numa.locks.hmcs.HMCSQNodeInterface.*;

public abstract class AbstractHMCSSleep<QNode extends HMCSQNodeInterfaceSleep> extends AbstractNumaLock {

    protected final HNode[] leafs;

    private final Supplier<QNode> qNodeSupplier;

    private final boolean oversubcription;

    @SuppressWarnings("unchecked")
    public AbstractHMCSSleep(Supplier<QNode> qNodeSupplier, Supplier<Integer> clusterIdSupplier, int leafsCnt, boolean oversubcription) {
        super(clusterIdSupplier);
        this.leafs = (HNode[]) Array.newInstance(HNode.class, leafsCnt);
        this.qNodeSupplier = qNodeSupplier;
        this.oversubcription = oversubcription;
    }

    @Override
    @SuppressWarnings("unchecked")
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
            qNode.setThreadAtomically(Thread.currentThread());
            HMCSQNodeInterfaceSleep pred = hNode.tail.getAndSet(qNode);
            if (pred == null) {
                qNode.setStatusAtomically(UNLOCKED);
            } else {
                pred.setNextAtomically(qNode);
                int spins = 256;
                while (qNode.getStatus() == LOCKED) {
                    spins--;
                    if (spins == 0 && oversubcription) {
                        LockSupport.park();
                        spins = 1;
                    }
                } // spin
            }
        } else {
            qNode.setNextAtomically(null);
            qNode.setStatusAtomically(WAIT);
            qNode.setThreadAtomically(Thread.currentThread());
            HMCSQNodeInterfaceSleep pred = hNode.tail.getAndSet(qNode);
            if (pred != null) {
                pred.setNextAtomically(qNode);
                int spins = 256;
                while (qNode.getStatus() == WAIT) {
                    spins--;
                    if (spins == 0 && oversubcription) {
                        LockSupport.park();
                        spins = 1;
                    }
                } // spin
                if (qNode.getStatus() < ACQUIRE_PARENT) {
                    return;
                }
            }
            qNode.setStatusAtomically(COHORT_START);
            lockH(hNode.node, hNode.parent);
        }
    }

    private void unlockH(HNode hNode, HMCSQNodeInterfaceSleep qNode) {
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
        HMCSQNodeInterfaceSleep succ = qNode.getNext();
        if (succ != null) {
            succ.setStatusAtomically(curCount + 1);
            if (oversubcription) {
                LockSupport.unpark(succ.getThread());
            }
            return;
        }
        unlockH(hNode.parent, hNode.node);
        releaseHelper(hNode, qNode, ACQUIRE_PARENT);
    }

    private void releaseHelper(HNode l, HMCSQNodeInterfaceSleep i, int val) {
        HMCSQNodeInterfaceSleep succ = i.getNext();
        if (succ != null) {
            succ.setStatusAtomically(val);
            if (oversubcription) {
                LockSupport.unpark(succ.getThread());
            }
        } else {
            if (l.tail.compareAndSet(i, null)) {
                return;
            }
            do {
                succ = i.getNext();
            } while (succ == null);
            succ.setStatusAtomically(val);
            if (oversubcription) {
                LockSupport.unpark(succ.getThread());
            }
        }
    }

    public class HNode {
        private final AtomicReference<HMCSQNodeInterfaceSleep> tail;
        private final HNode parent;
        QNode node;

        public HNode(HNode parent, QNode qNode) {
            this.parent = parent;
            this.tail = new AtomicReference<>(null);
            this.node = qNode;
        }
    }
}
