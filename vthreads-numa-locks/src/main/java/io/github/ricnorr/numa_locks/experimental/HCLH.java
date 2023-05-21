package io.github.ricnorr.numa_locks.experimental;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;

import jdk.internal.vm.annotation.Contended;

public class HCLH extends AbstractNumaLock<HCLH.HCLHLockCore.HCLHNode> {
  private final HCLHLockCore lockCore;

  public HCLH(Supplier<Integer> clusterIdSupplier) {
    super(clusterIdSupplier);
    this.lockCore = new HCLHLockCore();
  }

  @Override
  public HCLHLockCore.HCLHNode lock() {
    int clusterId = getClusterId();

    HCLHLockCore.HCLHNode node = new HCLHLockCore.HCLHNode();
    HCLHLockCore.HCLHNode myPred = lockCore.lock(node, clusterId);
    return node;
  }

  @Override
  public void unlock(HCLHLockCore.HCLHNode node) {
    lockCore.unlock(node);
  }

  public static class HCLHLockCore {
    static final int MAX_CLUSTERS = 35;
    final AtomicReferenceArray<HCLHNode> localQueues;
    final AtomicReference<HCLHNode> globalQueue;

    public HCLHLockCore() {
      localQueues = new AtomicReferenceArray<>(MAX_CLUSTERS);
      HCLHNode head = new HCLHNode();
      globalQueue = new AtomicReference<>(head);
    }

    public HCLHNode lock(HCLHNode myNode, int clusterID) {
      myNode.prepareForLock(clusterID);

      // splice my QNode into local queue
      HCLHNode myPred = localQueues.get(clusterID);
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
      HCLHNode localTail = localQueues.get(clusterID);
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

    public void unlock(HCLHNode myNode) {
      myNode.setSuccessorMustWait(false);
    }


    public static class HCLHNode {
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

      public HCLHNode() {
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

  }
}
