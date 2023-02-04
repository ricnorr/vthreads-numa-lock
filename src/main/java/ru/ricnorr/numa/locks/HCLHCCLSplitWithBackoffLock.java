package ru.ricnorr.numa.locks;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static ru.ricnorr.numa.locks.Utils.spinWaitYield;

public class HCLHCCLSplitWithBackoffLock extends AbstractLock {
    private final HCLHCCLSplitWithBackoffLockCore lockCore;

    ThreadLocal<HCLHCCLSplitWithBackoffLockCore.QNodeHCLH> prevNode = new ThreadLocal<>();

    ThreadLocal<HCLHCCLSplitWithBackoffLockCore.QNodeHCLH> currNode = ThreadLocal.withInitial(HCLHCCLSplitWithBackoffLockCore.QNodeHCLH::new);

    ThreadLocal<Integer> clusterID = ThreadLocal.withInitial(Utils::kungpengGetClusterID);

    public HCLHCCLSplitWithBackoffLock(HCLHCCLSplitWithBackoffLockSpec spec) {
        lockCore = new HCLHCCLSplitWithBackoffLockCore(spec);
    }

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

    public class HCLHCCLSplitWithBackoffLockCore {
        static final int MAX_CLUSTERS = 128;
        final List<AtomicReference<QNodeHCLH>> localQueues;
        final AtomicReference<HCLHCCLSplitWithBackoffLockCore.QNodeHCLH> globalQueue;

        final Backoff backoff1;
        final Backoff backoff2;
        final Backoff backoff3;

        final Backoff backoff4;

        public HCLHCCLSplitWithBackoffLockCore(HCLHCCLSplitWithBackoffLockSpec spec) {
            localQueues = new ArrayList<>();
            for (int i = 0; i < MAX_CLUSTERS; i++) {
                localQueues.add(new AtomicReference<>(null));
            }
            HCLHCCLSplitWithBackoffLockCore.QNodeHCLH head = new HCLHCCLSplitWithBackoffLockCore.QNodeHCLH();
            globalQueue = new AtomicReference<>(head);
            backoff1 = new Backoff(spec.minDelay1, spec.maxDelay1);
            backoff2 = new Backoff(spec.minDelay2, spec.maxDelay2);
            backoff3 = new Backoff(spec.minDelay3, spec.maxDelay3);
            backoff4 = new Backoff(spec.minDelay4, spec.maxDelay4);
        }

        public HCLHCCLSplitWithBackoffLockCore.QNodeHCLH lock(HCLHCCLSplitWithBackoffLockCore.QNodeHCLH myNode, Integer clusterID) {
            myNode.prepareForLock(clusterID);

            int index = clusterID;
            AtomicReference<HCLHCCLSplitWithBackoffLockCore.QNodeHCLH> localQueue = localQueues.get(index);
            // splice my QNode into local queue
            HCLHCCLSplitWithBackoffLockCore.QNodeHCLH myPred = localQueue.get();
            long lim1 = backoff1.minDelay;
            while (!localQueue.compareAndSet(myPred, myNode)) {
                lim1 = backoff1.backoff(lim1);
                myPred = localQueue.get();
            }
            if (myPred != null) {
                boolean iOwnLock = myPred.waitForGrantOrClusterMaster(backoff2, clusterID);
                if (iOwnLock) {
                    return myPred;
                }
            }
            // I am the cluster master: splice local queue into global queue.
            HCLHCCLSplitWithBackoffLockCore.QNodeHCLH localTail = localQueue.get();
            myPred = globalQueue.get();
            long lim3 = backoff3.minDelay;
            while (!globalQueue.compareAndSet(myPred, localTail)) {
                lim3 = backoff3.backoff(lim3);
                myPred = globalQueue.get();
                localTail = localQueue.get();
            }
            // inform successor it is the new master
            localTail.setTailWhenSpliced(true);
            long lim4 = backoff4.minDelay;
            while (myPred.isSuccessorMustWait()) {
                lim4 = backoff4.backoff(lim4);
            }

            return myPred;
        }

        public void unlock(HCLHCCLSplitWithBackoffLockCore.QNodeHCLH myNode) {
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

            boolean waitForGrantOrClusterMaster(Backoff backoff, Integer myCluster) {
                long lim = backoff.minDelay;
                while (true) {
                    if (getClusterID() == myCluster && !isTailWhenSpliced() && !isSuccessorMustWait()) {
                        return true;
                    } else if (getClusterID() != myCluster || isTailWhenSpliced()) {
                        return false;
                    }
                    lim = backoff.backoff(lim);
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
                int oldState = state.get();
                int newState;
                if (successorMustWait) {
                    newState = oldState | SMW_MASK;
                } else {
                    newState = oldState & ~SMW_MASK;
                }
                int spinCounter = 1;
                while (!state.compareAndSet(oldState, newState)) {
                    oldState = state.get();
                    if (successorMustWait) {
                        newState = oldState | SMW_MASK;
                    } else {
                        newState = oldState & ~SMW_MASK;
                    }
                    spinCounter = spinWaitYield(spinCounter);
                }
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

    public static class HCLHCCLSplitWithBackoffLockSpec {
        public long minDelay1 = 10;
        public long maxDelay1 = 100;

        public long minDelay2 = 10;
        public long maxDelay2 = 100;

        public long minDelay3 = 10;
        public long maxDelay3 = 100;

        public long minDelay4 = 10;
        public long maxDelay4 = 100;

        public static String MIN_DELAY_1 = "minDelay1";
        public static String MAX_DELAY_1 = "maxDelay1";
        public static String MIN_DELAY_2 = "minDelay2";
        public static String MAX_DELAY_2 = "maxDelay2";
        public static String MIN_DELAY_3 = "minDelay3";
        public static String MAX_DELAY_3 = "maxDelay3";
        public static String MIN_DELAY_4 = "minDelay4";
        public static String MAX_DELAY_4 = "maxDelay4";

        public HCLHCCLSplitWithBackoffLockSpec(String params) {
            var obj = (JSONObject) JSONValue.parse(params);
            if (obj.get(MIN_DELAY_1) != null) {
                this.minDelay1 = (Long) obj.get(MIN_DELAY_1);
            }
            if (obj.get(MAX_DELAY_1) != null) {
                this.maxDelay1 = (Long) obj.get(MAX_DELAY_1);
            }
            if (obj.get(MIN_DELAY_2) != null) {
                this.minDelay2 = (Long) obj.get(MIN_DELAY_2);
            }
            if (obj.get(MAX_DELAY_2) != null) {
                this.maxDelay2 = (Long) obj.get(MAX_DELAY_2);
            }
            if (obj.get(MIN_DELAY_3) != null) {
                this.minDelay3 = (Long) obj.get(MIN_DELAY_3);
            }
            if (obj.get(MAX_DELAY_3) != null) {
                this.maxDelay3 = (Long) obj.get(MAX_DELAY_3);
            }
            if (obj.get(MIN_DELAY_4) != null) {
                this.minDelay4 = (Long) obj.get(MIN_DELAY_4);
            }
            if (obj.get(MAX_DELAY_4) != null) {
                this.maxDelay4 = (Long) obj.get(MAX_DELAY_4);
            }
        }
    }

    private class Backoff {
        final long minDelay, maxDelay;
        final long limit;

        public Backoff(long min, long max) {
            minDelay = min;
            maxDelay = max;
            limit = minDelay;
        }

        public long backoff(long limit) {
            long delay = ThreadLocalRandom.current().nextLong(limit);
            limit = Math.min(maxDelay, 2 * limit);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
            return limit;
        }
    }
}
