package ru.ricnorr.numa.locks.combination;

import jdk.internal.vm.annotation.Contended;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.AbstractNumaLock;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static ru.ricnorr.numa.locks.combination.CombinationLock.HNode.NO_LOCK;

public class CombinationLock extends AbstractNumaLock {
    final CombinationLockCore lockCore;

    public CombinationLock(List<CombinationLockLevelDescription> levelToDescription, Supplier<Integer> lastLvlClusterIdSupplier) {
        super(lastLvlClusterIdSupplier);
        lockCore = new CombinationLockCore(levelToDescription);
    }

    @Override
    public Object lock(Object objForLock) {
        int clusterId = getClusterId();
        lockCore.lock(clusterId);
        return clusterId;
    }

    @Override
    public void unlock(Object objForUnlock) {
        int clusterId = (int) (objForUnlock);
        lockCore.unlock(clusterId);
    }

    @Override
    public boolean hasNext(Object obj) {
        return false;
    }

    public static class HNode {

        public static int NO_LOCK = 0;


        private final NumaLock numaLock;

        private final HNode parent;

        @Contended
        private volatile Object objForUnlock = null;

        private volatile int status = NO_LOCK;

        public HNode(HNode parent, NumaLock numaLock) {
            this.parent = parent;
            this.numaLock = numaLock;
            if (numaLock.canUseNodeFromPreviousLocking()) {
                this.objForUnlock = numaLock.supplyNode();
            }
        }
    }

    public static class CombinationLockCore {
        private final HNode[] leafs;

        public CombinationLockCore(List<CombinationLockLevelDescription> levelToDescription) {
            CombinationLockLevelDescription curDescription = levelToDescription.get(0);
            List<HNode> curLvlNodes = List.of(new HNode(null, Utils.initLock(curDescription.lockType)));
            for (int i = 0; i < levelToDescription.size() - 1; i++) {
                CombinationLockLevelDescription nextDescription = levelToDescription.get(i + 1);
                List<HNode> nodesOnNextLvl = new ArrayList<>();
                for (HNode nodeFromCurLvl : curLvlNodes) {
                    for (int j = 0; j < curDescription.childrenOnNextLevel; j++) {
                        nodesOnNextLvl.add(new HNode(nodeFromCurLvl, Utils.initLock(nextDescription.lockType)));
                    }
                }
                curLvlNodes = nodesOnNextLvl;
                curDescription = nextDescription;
            }
            leafs = curLvlNodes.toArray(new HNode[0]);
        }

        public void lock(int clusterId) {
            lockH(leafs[clusterId]);
        }

        private void lockH(HNode hNode) {
            hNode.objForUnlock = hNode.numaLock.lock(null);
            int status = hNode.status;
            if (hNode.parent == null) {
                return;
            }
            if (status == NO_LOCK) {
                lockH(hNode.parent);
            }
        }

        public void unlock(int clusterId) {
            unlockH(leafs[clusterId]);
        }

        private void unlockH(HNode hNode) {
            boolean hasNext = hNode.numaLock.hasNext(hNode.objForUnlock);
            int status = hNode.status;
            if (hasNext && status < 10000) {
                hNode.status = status + 1;
            } else {
                hNode.status = NO_LOCK;
                if (hNode.parent != null) {
                    unlockH(hNode.parent);
                }
            }
            hNode.numaLock.unlock(hNode.objForUnlock);
        }
    }

    public static class CombinationLockLevelDescription {
        public LockType lockType;

        public Integer childrenOnNextLevel;

        public CombinationLockLevelDescription(LockType lockType, Integer childrenOnNextLevel) {
            this.lockType = lockType;
            this.childrenOnNextLevel = childrenOnNextLevel;
        }
    }
}
