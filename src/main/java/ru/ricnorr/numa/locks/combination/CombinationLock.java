package ru.ricnorr.numa.locks.combination;

import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.AbstractNumaLock;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static ru.ricnorr.numa.locks.combination.CombinationLock.HNode.NO_LOCK;

public class CombinationLock extends AbstractNumaLock {
    final HNode[] leafs;

    public CombinationLock(List<CombinationLockLevelDescription> levelToDescription, Supplier<Integer> lastLvlClusterIdSupplier) {
        super(lastLvlClusterIdSupplier);
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

    @Override
    public Object lock(Object objForLock) {
        int clusterId = getClusterId();
        lockH(leafs[clusterId]);
        return clusterId;
    }

    @Override
    public void unlock(Object objForUnlock) {
        int clusterId = (int) (objForUnlock);
        unlockH(leafs[clusterId]);
    }

    @Override
    public boolean hasNext(Object obj) {
        return false;
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

    public static class HNode {

        public static int NO_LOCK = 0;


        private final NumaLock numaLock;

        private final HNode parent;
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


    public static class CombinationLockLevelDescription {
        public LockType lockType;

        public Integer childrenOnNextLevel;

        public CombinationLockLevelDescription(LockType lockType, Integer childrenOnNextLevel) {
            this.lockType = lockType;
            this.childrenOnNextLevel = childrenOnNextLevel;
        }
    }
}
