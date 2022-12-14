package ru.ricnorr.numa.locks;

import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;



public class HCLHLock extends AbstractLock {

    private final static int GET_CPU_ARM_SYSCALL = 168;

    private final static int GET_CPU_x86_SYSCALL = 309;
    private final HCLHLockCore lockCore = new HCLHLockCore();

    ThreadLocal<HCLHLockCore.QNodeHCLH> prevNode = new ThreadLocal<>();

    ThreadLocal<HCLHLockCore.QNodeHCLH> currNode = ThreadLocal.withInitial(HCLHLockCore.QNodeHCLH::new);

    ThreadLocal<Integer> clusterID = ThreadLocal.withInitial(() -> {
        int res;
        final IntByReference numaNode = new IntByReference();

        if (Platform.isARM()) {
            res = CLibrary.INSTANCE.syscall(GET_CPU_ARM_SYSCALL, null, numaNode, null);
        } else {
            res = CLibrary.INSTANCE.syscall(GET_CPU_x86_SYSCALL, null, numaNode,null);
        }
        if (res < 0) {
            throw new IllegalStateException("Cannot make syscall getcpu");
        }
        return numaNode.getValue();
    });

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
            QNodeHCLH myPred = null;
            do {
                myPred = localQueue.get();
            } while (!localQueue.compareAndSet(myPred, myNode));
            if (myPred != null) {
                boolean iOwnLock = myPred.waitForGrantOrClusterMaster(clusterID);
                if (iOwnLock) {
                    return myPred;
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
            while (myPred.isSuccessorMustWait()) { //isSuccessorMustWait()) {
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
            final AtomicInteger state;

            public QNodeHCLH() {
                state = new AtomicInteger(0);
            }

            boolean waitForGrantOrClusterMaster(Integer myCluster) {
                while (true) {
                    if (getClusterID() == myCluster && !isTailWhenSpliced() && !isSuccessorMustWait()) {
                        return true;
                    } else if (getClusterID() != myCluster || isTailWhenSpliced()) {
                        return false;
                    }
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

}

