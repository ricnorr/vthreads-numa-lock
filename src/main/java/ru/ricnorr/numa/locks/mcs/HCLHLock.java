package ru.ricnorr.numa.locks.mcs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class HCLHLock implements Lock {

    //private static ThreadLocal<Integer> threadID = new ThreadLocal<>();
    static final int MAX_CLUSTERS = 3;
    List<AtomicReference<QNodeHCLH>> localQueues;
    AtomicReference<QNodeHCLH> globalQueue;
    ThreadLocal<QNodeHCLH> currNode = ThreadLocal.withInitial(QNodeHCLH::new);
    ThreadLocal<QNodeHCLH> predNode = ThreadLocal.withInitial(() -> null);

    private static int getClusterId() {
        return 0;//(int)Thread.currentThread().getId() % 3;
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
        AtomicReference<QNodeHCLH> localQueue = localQueues.get(getClusterId());
        // splice my QNode into local queue
        QNodeHCLH myPred = null;
        do {
            myPred = localQueue.get(); // myPred - захватили локальный хвостик
        } while (!localQueue.compareAndSet(myPred, myNode));
        if (myPred != null) { // мы первые в локальной очереди
            boolean iOwnLock = myPred.waitForGrantOrClusterMaster(); // grant - это значит нам передали лок локально
            if (iOwnLock) {                                          // cluster master - значит что мы первые в локальной очереди, но не факт что владеем локом
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
        while (myPred.isSuccessorMustWait()) { // ждем на предыдущем из глобальной очереди
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
        // private boolean tailWhenSpliced;
        private static final int TWS_MASK = 0x80000000;
        // private boolean successorMustWait= false;
        private static final int SMW_MASK = 0x40000000;
        // private int clusterID;
        private static final int CLUSTER_MASK = 0x3FFFFFFF;
        AtomicInteger state;

        public QNodeHCLH() {
            state = new AtomicInteger(0);
        }

        boolean waitForGrantOrClusterMaster() {
            int myCluster = HCLHLock.getClusterId();
            while (true) {
                if (getClusterID() == myCluster && !isTailWhenSpliced() && !isSuccessorMustWait()) {
                    return true;
                } else if (getClusterID() != myCluster || isTailWhenSpliced()) { // если getClusterId() != myCluster это значит, что узел уже перенесли в глобальную очередь и переработали
                    // если isTailWhenSpliced() == true, значит нашего предка уже перенесли
                    return false;
                }
            }
        }

        public void unlock() {
            int oldState = 0;
            int newState = getClusterID();
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

        public void setClusterID(int clusterID) {
            int oldState, newState;
            do {
                oldState = state.get();
                newState = (oldState & ~CLUSTER_MASK) | clusterID;
            } while (!state.compareAndSet(oldState, newState));
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
                    newState = oldState & TWS_MASK;
                }
            } while (!state.compareAndSet(oldState, newState));
        }
    }

}
