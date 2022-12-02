package ru.ricnorr.numa.locks.mcs;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class HCLHLock implements Lock {

    static final int MAX_CLUSTERS = 3;
    List<AtomicReference<QNodeHCLH>> localQueues;
    AtomicReference<QNodeHCLH> globalQueue;
    ThreadLocal<QNodeHCLH> currNode = new ThreadLocal<QNodeHCLH>() {
        protected QNodeHCLH initialValue() {
            return new QNodeHCLH();
        }
    };

    ThreadLocal<QNodeHCLH> predNode = ThreadLocal.withInitial(() -> null);

    ThreadLocal<Integer> threadID = new ThreadLocal<>() {
        protected Integer initialValue() {
            return Math.abs(new Random().nextInt()) % MAX_CLUSTERS;
        }
    };


    private int getClusterId() {
        return threadID.get();
    }

    public HCLHLock() {
        localQueues = new ArrayList<>(MAX_CLUSTERS);
        for (int i = 0; i < MAX_CLUSTERS; i++) {
            localQueues.add(new AtomicReference<>());
        }
        QNodeHCLH head = new QNodeHCLH();
        globalQueue = new AtomicReference<>(head);
    }

    @Override
    public void lock() {
        QNodeHCLH myNode = currNode.get();
        myNode.setSuccessorMustWait(true);
        AtomicReference<QNodeHCLH> localQueue = localQueues.get(getClusterId());
        // splice my QNode into local queue
        QNodeHCLH myPred = null;
        do {
            myPred = localQueue.get();
        } while (!localQueue.compareAndSet(myPred, myNode));
        if (myPred != null) {
            boolean iOwnLock = myPred.waitForGrantOrClusterMaster();
            if (iOwnLock) {
                predNode.set(myPred);
                return;
            }
        }
        // I am the cluster master: splice local queue into global queue.
        QNodeHCLH localTail = null;
        do {
            myPred = globalQueue.get();
            localTail = localQueue.get();
        } while (!globalQueue.compareAndSet(myPred, localTail));
        // inform successor it is the new master
        localTail.setTailWhenSpliced(true);
        while (myPred.isSuccessorMustWait()) {
        }
        predNode.set(myPred);
    }

    @Override
    public void unlock() {
        QNodeHCLH myNode = currNode.get();
        myNode.setSuccessorMustWait(false);
        QNodeHCLH node = predNode.get();
        node.unlock();
        currNode.set(node);
    }

    @Override
    public void lockInterruptibly() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean tryLock() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Condition newCondition() {
        throw new RuntimeException("Not implemented");
    }

    class QNodeHCLH {
        //private boolean tailWhenSpliced;
        private static final int TWS_MASK = 0x80000000;
        //10000000000000000000000000000000

        // private boolean successorMustWait= false;
        private static final int SMW_MASK = 0x40000000;
        //01000000000000000000000000000000

        // private int clusterID;
        private static final int CLUSTER_MASK = 0x3FFFFFFF;
        //00111111111111111111111111111111
        AtomicInteger state;

        public QNodeHCLH() {
            state = new AtomicInteger(0);
        }

        boolean waitForGrantOrClusterMaster() {
            int myCluster = getClusterId();
            while (true) {
                if (getClusterID() == myCluster && !isTailWhenSpliced() && !isSuccessorMustWait()) {
                    return true;
                } else if (getClusterID() != myCluster || isTailWhenSpliced()) {
                    return false;
                }
            }
        }

        public void unlock() {
            int oldState = 0;
            int newState = getClusterId();
            // successorMustWait = true;
            newState |= SMW_MASK;
            // tailWhenSpliced = false;
            newState &= (~TWS_MASK);
            do {
                oldState = state.get();
            } while (!state.compareAndSet(oldState, newState));
        }

        public int getClusterID() {
            return state.get() & CLUSTER_MASK;
        }

        public boolean isSuccessorMustWait() {
            return (state.get() & SMW_MASK) != 0;
        }

        public void setSuccessorMustWait(boolean successorMustWait) {
            int oldState, newState;
            do {
                oldState = state.get();
                if (successorMustWait) {
                    newState = oldState | SMW_MASK;
                } else {
                    newState = oldState & ~SMW_MASK;
                }
            } while (!state.compareAndSet(oldState, newState));
        }

        public boolean isTailWhenSpliced() {
            return (state.get() & TWS_MASK) != 0;
        }

        public void setTailWhenSpliced(boolean tailWhenSpliced) {
            int oldState, newState;
            do {
                oldState = state.get();
                if (tailWhenSpliced) {
                    newState = oldState | TWS_MASK;
                } else {
                    newState = oldState & ~TWS_MASK;
                }
            } while (!state.compareAndSet(oldState, newState));
        }
    }

}
