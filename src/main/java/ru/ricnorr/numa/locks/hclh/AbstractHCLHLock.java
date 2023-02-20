package ru.ricnorr.numa.locks.hclh;

import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class AbstractHCLHLock implements NumaLock {
    private final HCLHLockCore lockCore = new HCLHLockCore();

    ThreadLocal<HCLHLockCore.QNodeHCLH> prevNode = new ThreadLocal<>();

    ThreadLocal<HCLHLockCore.QNodeHCLH> currNode = ThreadLocal.withInitial(HCLHLockCore.QNodeHCLH::new);

    ThreadLocal<Integer> clusterID;

    private final boolean isLight;

    public AbstractHCLHLock(boolean isLight, Supplier<Integer> clusterIdSupplier) {
        this.isLight = isLight;
        clusterID = ThreadLocal.withInitial(clusterIdSupplier);
    }

    @Override
    public Object lock() {
        if (isLight) {
            var node = new HCLHLockCore.QNodeHCLH();
            lockCore.lock(node, Utils.getByThreadFromThreadLocal(clusterID, Utils.getCurrentCarrierThread()));
            return node;
        } else {
            var myPred = lockCore.lock(currNode.get(), clusterID.get());
            prevNode.set(myPred);
            return null;
        }

    }

    @Override
    public void unlock(Object t) {
        if (isLight) {
            lockCore.unlock((HCLHLockCore.QNodeHCLH) t);
        } else {
            lockCore.unlock(currNode.get());
            currNode.set(prevNode.get());
            prevNode.set(null);
        }
    }

    public static class HCLHLockCore {
        static final int MAX_CLUSTERS = 35;
        final List<AtomicReference<QNodeHCLH>> localQueues;
        final AtomicReference<QNodeHCLH> globalQueue;

        public HCLHLockCore() {
            localQueues = new ArrayList<>();
            for (int i = 0; i < MAX_CLUSTERS; i++) {
                localQueues.add(new AtomicReference<>());
            }
            QNodeHCLH head = new QNodeHCLH();
            globalQueue = new AtomicReference<>(head);
        }

        public QNodeHCLH lock(QNodeHCLH myNode, Integer clusterID) {
            myNode.prepareForLock(clusterID);

            int index = clusterID;
            AtomicReference<QNodeHCLH> localQueue = localQueues.get(index);
            // splice my QNode into local queue
            QNodeHCLH myPred = localQueue.get();
            while (!localQueue.compareAndSet(myPred, myNode)) {
                myPred = localQueue.get();
            }
            if (myPred != null) {
                boolean iOwnLock = myPred.waitForGrantOrClusterMaster(clusterID);
                if (iOwnLock) {
                    return myPred;
                }
            }
            // I am the cluster master: splice local queue into global queue.
            QNodeHCLH localTail = localQueue.get();
            myPred = globalQueue.get();
            while (!globalQueue.compareAndSet(myPred, localTail)) {
                myPred = globalQueue.get();
                localTail = localQueue.get();
            }
            // inform successor it is the new master
            localTail.setTailWhenSpliced(true);
            while (myPred.isSuccessorMustWait()) {
                Thread.onSpinWait();
            }

            return myPred;
        }

        public void unlock(QNodeHCLH myNode) {
            myNode.setSuccessorMustWait(false);
        }

        public static class QNodeHCLH {
            //private boolean tailWhenSpliced;
            private static final int TWS_MASK = 0x80000000;
            //10000000000000000000000000000000

            // private boolean successorMustWait= false;
            private static final int SMW_MASK = 0x40000000;
            //01000000000000000000000000000000

            // private int clusterID;
            private static final int CLUSTER_MASK = 0x3FFFFFFF;
            //00111111111111111111111111111111
            volatile int state;

            public QNodeHCLH() {
                state = 0; //new AtomicInteger(0);
            }

            boolean waitForGrantOrClusterMaster(Integer myCluster) {
                while (true) {
                    if (getClusterID() == myCluster && !isTailWhenSpliced() && !isSuccessorMustWait()) {
                        return true;
                    } else if (getClusterID() != myCluster || isTailWhenSpliced()) {
                        return false;
                    }
                    Thread.onSpinWait();
                }
            }

            public void prepareForLock(int clusterId) {
                int oldState = 0;
                int newState = clusterId;
                // successorMustWait = true;
                newState |= SMW_MASK;
                // tailWhenSpliced = false;
                newState &= (~TWS_MASK);
                state = newState;
//                do {
//                    oldState = state.get();
//                } while (!state.compareAndSet(oldState, newState));
            }

            public int getClusterID() {
//                return state.get() & CLUSTER_MASK;
                return state & CLUSTER_MASK;
            }

            public boolean isSuccessorMustWait() {
                return (state & SMW_MASK) != 0; //(state.get() & SMW_MASK) != 0;
            }

            public void setSuccessorMustWait(boolean successorMustWait) {
                int oldState = state;
                int newState;
                if (successorMustWait) {
                    newState = oldState | SMW_MASK;
                } else {
                    newState = oldState & ~SMW_MASK;
                }
                state = newState;
//                int spinCounter = 1;
//                while (!state.compareAndSet(oldState, newState)) {
//                    oldState = state.get();
//                    if (successorMustWait) {
//                        newState = oldState | SMW_MASK;
//                    } else {
//                        newState = oldState & ~SMW_MASK;
//                    }
//                    spinCounter = spinWaitYield(spinCounter);
//                }
            }

            public boolean isTailWhenSpliced() {
                return (state & TWS_MASK) != 0;
            }

            public void setTailWhenSpliced(boolean tailWhenSpliced) {
                int oldState = state;
                int newState;
                if (tailWhenSpliced) {
                    newState = oldState | TWS_MASK;
                } else {
                    newState = oldState & ~TWS_MASK;
                }
                state = newState;
            }
        }
    }
}
