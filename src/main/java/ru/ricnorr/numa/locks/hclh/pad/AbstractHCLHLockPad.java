package ru.ricnorr.numa.locks.hclh.pad;

import jdk.internal.vm.annotation.Contended;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;

public class AbstractHCLHLockPad implements NumaLock {
    private final HCLHLockCore lockCore = new HCLHLockCore();

    ThreadLocal<HCLHLockCore.QNodeHCLHPad> prevNode = new ThreadLocal<>();

    ThreadLocal<HCLHLockCore.QNodeHCLHPad> currNode = ThreadLocal.withInitial(HCLHLockCore.QNodeHCLHPad::new);

    ThreadLocal<Integer> clusterID;

    private final boolean isLight;

    public AbstractHCLHLockPad(boolean isLight, Supplier<Integer> clusterIdSupplier) {
        this.isLight = isLight;
        clusterID = ThreadLocal.withInitial(clusterIdSupplier);
    }

    @Override
    public Object lock() {
        if (isLight) {
            var node = new HCLHLockCore.QNodeHCLHPad();
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
            lockCore.unlock((HCLHLockCore.QNodeHCLHPad) t);
        } else {
            lockCore.unlock(currNode.get());
            currNode.set(prevNode.get());
            prevNode.set(null);
        }
    }

    public static class HCLHLockCore {
        static final int MAX_CLUSTERS = 35;
        final AtomicReferenceArray<QNodeHCLHPad> localQueues;
        final AtomicReference<QNodeHCLHPad> globalQueue;

        public HCLHLockCore() {
            localQueues = new AtomicReferenceArray<>(MAX_CLUSTERS);
            QNodeHCLHPad head = new QNodeHCLHPad();
            globalQueue = new AtomicReference<>(head);
        }

        public QNodeHCLHPad lock(QNodeHCLHPad myNode, Integer clusterID) {
            myNode.prepareForLock(clusterID);

            int index = clusterID;
            // splice my QNode into local queue
            QNodeHCLHPad myPred = localQueues.get(index);
            while (!localQueues.compareAndSet(index, myPred, myNode)) {
                Thread.onSpinWait();
                myPred = localQueues.get(index);
            }
            if (myPred != null) {
                boolean iOwnLock = myPred.waitForGrantOrClusterMaster(clusterID);
                if (iOwnLock) {
                    return myPred;
                }
            }
            // I am the cluster master: splice local queue into global queue.
            QNodeHCLHPad localTail = localQueues.get(index);
            myPred = globalQueue.get();
            while (!globalQueue.compareAndSet(myPred, localTail)) {
                Thread.onSpinWait();
                myPred = globalQueue.get();
                localTail = localQueues.get(index);
            }
            // inform successor it is the new master
            localTail.setTailWhenSpliced(true);
            while (myPred.isSuccessorMustWait()) {
                Thread.onSpinWait();
            }

            return myPred;
        }

        public void unlock(QNodeHCLHPad myNode) {
            myNode.setSuccessorMustWait(false);
        }

        @Contended
        public static class QNodeHCLHPad {
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

            public QNodeHCLHPad() {
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
