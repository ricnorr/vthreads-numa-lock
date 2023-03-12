package ru.ricnorr.numa.locks.hclh;

import ru.ricnorr.numa.locks.AbstractNumaLock;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;

public class AbstractHCLHLock<NodeType extends HCLHNodeInterface> extends AbstractNumaLock {
    private final HCLHLockCore<NodeType> lockCore;
    private final Supplier<NodeType> nodeFactory;

    public AbstractHCLHLock(Supplier<NodeType> nodeFactory, Supplier<Integer> clusterIdSupplier) {
        super(clusterIdSupplier);
        this.nodeFactory = nodeFactory;
        this.lockCore = new HCLHLockCore<>(nodeFactory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object lock(Object obj) {
        int clusterId = getClusterId();

        NodeType node;
        if (obj != null) {
            node = (NodeType) obj;
        } else {
            node = nodeFactory.get();
        }

        NodeType myPred = lockCore.lock(node, clusterId);
        return node;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unlock(Object t) {
        lockCore.unlock((NodeType) t);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean hasNext(Object obj) {
        NodeType node = (NodeType) obj;
        int clusterId = node.getClusterID();
        NodeType localQueueTail = lockCore.localQueues.get(clusterId);
        NodeType globalQueueTail = lockCore.globalQueue.get();
        return (localQueueTail != null && localQueueTail != node) || (globalQueueTail != null && globalQueueTail != node);
    }

    public static class HCLHLockCore<Node extends HCLHNodeInterface> {
        static final int MAX_CLUSTERS = 35;
        final AtomicReferenceArray<Node> localQueues;
        final AtomicReference<Node> globalQueue;

        public HCLHLockCore(Supplier<Node> nodeSupplier) {
            localQueues = new AtomicReferenceArray<>(MAX_CLUSTERS);
            Node head = nodeSupplier.get(); // new HCLHNodeNoPad();
            globalQueue = new AtomicReference<>(head);
        }

        public Node lock(Node myNode, int clusterID) {
            myNode.prepareForLock(clusterID);

            // splice my QNode into local queue
            Node myPred = localQueues.get(clusterID);
            while (!localQueues.compareAndSet(clusterID, myPred, myNode)) {
                Thread.onSpinWait();
                myPred = localQueues.get(clusterID);
            }
            if (myPred != null) {
                boolean iOwnLock = myPred.waitForGrantOrClusterMaster(clusterID);
                if (iOwnLock) {
                    return myPred;
                }
            }
            // I am the cluster master: splice local queue into global queue.
            Node localTail = localQueues.get(clusterID);
            myPred = globalQueue.get();
            while (!globalQueue.compareAndSet(myPred, localTail)) {
                Thread.onSpinWait();
                myPred = globalQueue.get();
                localTail = localQueues.get(clusterID);
            }
            // inform successor it is the new master
            localTail.setTailWhenSpliced(true);
            while (myPred.isSuccessorMustWait()) {
                Thread.onSpinWait();
            }

            return myPred;
        }

        public void unlock(Node myNode) {
            myNode.setSuccessorMustWait(false);
        }
    }
}
