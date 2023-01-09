package ru.ricnorr.numa.locks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import kotlinx.atomicfu.AtomicArray;
import kotlinx.atomicfu.AtomicInt;
import kotlinx.atomicfu.AtomicRef;
import ru.ricnorr.numa.locks.Utils.*;

import static kotlinx.atomicfu.AtomicFU.atomic;
import static ru.ricnorr.numa.locks.Utils.spinWait;


public class HCLHLock extends AbstractLock {

    private final HCLHLockCore lockCore = new HCLHLockCore();

    ThreadLocal<HCLHLockCore.QNodeHCLH> prevNode = new ThreadLocal<>();

    ThreadLocal<HCLHLockCore.QNodeHCLH> currNode = ThreadLocal.withInitial(HCLHLockCore.QNodeHCLH::new);

    ThreadLocal<Integer> clusterID = ThreadLocal.withInitial(Utils::getClusterID);

    @Override
    public void lock() {
        var myPred = lockCore.lock(currNode.get(), clusterID.get());
        prevNode.set(myPred);
    }

    @Override
    public void unlock() {
        lockCore.unlock(currNode.get());
        currNode.set(prevNode.get());
        prevNode.set(null);
    }

    public static class HCLHLockCore {
        static final int MAX_CLUSTERS = 25;
        final AtomicArray<QNodeHCLH> localQueues;
        final AtomicRef<QNodeHCLH> globalQueue;

        public HCLHLockCore() {
            localQueues = new AtomicArray<>(MAX_CLUSTERS);
            QNodeHCLH head = new QNodeHCLH();
            globalQueue = atomic(head);
        }

        public QNodeHCLH lock(QNodeHCLH myNode, Integer clusterID) {
            myNode.prepareForLock(clusterID);

            int index = clusterID;
            AtomicRef<QNodeHCLH> localQueue = localQueues.get(index);
            // splice my QNode into local queue
            QNodeHCLH myPred = localQueue.getValue();
            int spinCounter = 1;
            while (!localQueue.compareAndSet(myPred, myNode)) {
                spinCounter = spinWait(spinCounter);
                myPred = localQueue.getValue();
            }
            if (myPred != null) {
                boolean iOwnLock = myPred.waitForGrantOrClusterMaster(clusterID);
                if (iOwnLock) {
                    return myPred;
                }
            }
            // I am the cluster master: splice local queue into global queue.
            QNodeHCLH localTail = localQueue.getValue();
            myPred = globalQueue.getValue();
            spinCounter = 1;
            while (!globalQueue.compareAndSet(myPred, localTail)) {
                spinCounter = spinWait(spinCounter);
                myPred = globalQueue.getValue();
                localTail = localQueue.getValue();
            }
            // inform successor it is the new master
            localTail.setTailWhenSpliced(true);
            spinCounter = 1;
            while (myPred.isSuccessorMustWait()) {
                spinCounter = spinWait(spinCounter);
            }

            return myPred;
        }

        public void unlock(QNodeHCLH myNode) {
            myNode.setSuccessorMustWait(false);
            Thread thread = myNode.thread.getValue();
            if (thread != null) {
                LockSupport.unpark(thread);
            }
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
            final AtomicInt state;

            final AtomicRef<Thread> thread = atomic(null);

            public QNodeHCLH() {
                state = atomic(0);
            }

            boolean waitForGrantOrClusterMaster(Integer myCluster) {
                thread.setValue(Thread.currentThread());
                while (true) {
                    if (getClusterID() == myCluster && !isTailWhenSpliced() && !isSuccessorMustWait()) {
                        return true;
                    } else if (getClusterID() != myCluster || isTailWhenSpliced()) {
                        return false;
                    }
                    LockSupport.park(this);
                }
            }
            public void prepareForLock(int clusterId) {
                int oldState = 0;
                int newState = clusterId;
                // successorMustWait = true;
                newState |= SMW_MASK;
                // tailWhenSpliced = false;
                newState &= (~TWS_MASK);
                do {
                    oldState = state.getValue();
                } while (!state.compareAndSet(oldState, newState));
                thread.setValue(null);
            }

            public int getClusterID() {
                return state.getValue() & CLUSTER_MASK;
            }

            public boolean isSuccessorMustWait() {
                return (state.getValue() & SMW_MASK) != 0;
            }

            public void setSuccessorMustWait(boolean successorMustWait) {
                int oldState = state.getValue();
                int newState;
                if (successorMustWait) {
                    newState = oldState | SMW_MASK;
                } else {
                    newState = oldState & ~SMW_MASK;
                }
                int spinCounter = 1;
                while (!state.compareAndSet(oldState, newState)) {
                    oldState = state.getValue();
                    if (successorMustWait) {
                        newState = oldState | SMW_MASK;
                    } else {
                        newState = oldState & ~SMW_MASK;
                    }
                    spinCounter = spinWait(spinCounter);
                }
            }

            public boolean isTailWhenSpliced() {
                return (state.getValue() & TWS_MASK) != 0;
            }

            public void setTailWhenSpliced(boolean tailWhenSpliced) {
                int oldState, newState;
                do {
                    oldState = state.getValue();
                    if (tailWhenSpliced) {
                        newState = oldState | TWS_MASK;
                    } else {
                        newState = oldState & ~TWS_MASK;
                    }
                } while (!state.compareAndSet(oldState, newState));
            }
        }
    }

}

