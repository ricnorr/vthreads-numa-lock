package ru.ricnorr.numa.locks.hclh;

import jdk.internal.vm.annotation.Contended;

public class HCLHNodeWithPad implements HCLHNodeInterface {
    private static final int TWS_MASK = 0x80000000;
    //10000000000000000000000000000000

    // private boolean successorMustWait= false;
    private static final int SMW_MASK = 0x40000000;
    //01000000000000000000000000000000

    // private int clusterID;
    private static final int CLUSTER_MASK = 0x3FFFFFFF;
    //00111111111111111111111111111111

    @Contended
    volatile int state;

    public HCLHNodeWithPad() {
        state = 0;
    }

    public boolean waitForGrantOrClusterMaster(Integer myCluster) {
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
    }

    public int getClusterID() {
        return state & CLUSTER_MASK;
    }

    public boolean isSuccessorMustWait() {
        return (state & SMW_MASK) != 0;
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
