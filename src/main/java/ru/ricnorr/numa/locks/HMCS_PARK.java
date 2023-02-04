package ru.ricnorr.numa.locks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static ru.ricnorr.numa.locks.HMCS_PARK.QNode.*;


public class HMCS_PARK extends AbstractLock {
    public static class HNode {
        private HNode parent;

        private final AtomicReference<QNode> tail;

        QNode node;

        public HNode(HNode parent) {
            this.parent = parent;
            this.tail = new AtomicReference<>(null);
            this.node = new QNode();
        }
    }

    public static class QNode {

        static int WAIT = Integer.MAX_VALUE;
        static int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
        static int UNLOCKED = 0x0;
        static int LOCKED = 0x1;
        static int COHORT_START = 0x1;

        private final AtomicReference<QNode> next = new AtomicReference<>(null);
        private final AtomicInteger status = new AtomicInteger(WAIT);

        private final AtomicReference<Thread> thread = new AtomicReference<>(null);

    }

    final List<HNode> leafs;

    final ThreadLocal<QNode> localQNode = ThreadLocal.withInitial(QNode::new);

    final ThreadLocal<Integer> localClusterID = ThreadLocal.withInitial(Utils::kungpengGetClusterID);

    public HMCS_PARK(HMCSLockSpec spec) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int treeHeight = 4;
        int cclSize = (int) spec.ccl;
        List<List<HNode>> levels = new ArrayList<>();

        {
            int lastLvlCnt = availableProcessors / cclSize;
            List<HNode> lastLvl = new ArrayList<>();
            for (int i = 0; i < lastLvlCnt; i++) {
                lastLvl.add(new HNode(null));
            }
            levels.add(lastLvl);
        }
        for (int i = 0; i < treeHeight - 1; i++) {
            List<HNode> curLvl = levels.get(levels.size() - 1);
            int nextLvlCnt = (int) Math.pow(2, treeHeight - i - 2);
            List<HNode> nextLvl = new ArrayList<>();
            for (int j = 0; j < nextLvlCnt; j++) {
                nextLvl.add(new HNode(null));
            }
            int packSize = curLvl.size() / nextLvlCnt;
            for (int j = 0; j < nextLvlCnt; j++) {
                for (int k = 0; k < packSize; k++) {
                    curLvl.get(packSize * j + k).parent = nextLvl.get(j);
                }
            }
            levels.add(nextLvl);
        }
        leafs = levels.get(0);
    }

    @Override
    public void lock() {
        lockH(localQNode.get(), leafs.get(localClusterID.get()));
    }

    @Override
    public void unlock() {
        unlockH(leafs.get(localClusterID.get()), localQNode.get());
    }

    private void lockH(QNode qNode, HNode hNode) {
        if (hNode.parent == null) {
            qNode.next.set(null);
            qNode.status.set(LOCKED);
            qNode.thread.set(Thread.currentThread());
            QNode pred = hNode.tail.getAndSet(qNode);
            if (pred == null) {
                qNode.status.set(UNLOCKED);
            } else {
                pred.next.set(qNode);
                while (qNode.status.get() == LOCKED) {
                    LockSupport.park(this);
                } // spin
            }
        } else {
            qNode.next.set(null);
            qNode.status.set(WAIT);
            qNode.thread.set(Thread.currentThread());
            QNode pred = hNode.tail.getAndSet(qNode);
            if (pred != null) {
                pred.next.set(qNode);
                while (qNode.status.get() == WAIT) {
                    LockSupport.park(this);
                } // spin
                if (qNode.status.get() < ACQUIRE_PARENT) {
                    return;
                }
            }
            qNode.status.set(COHORT_START);
            lockH(hNode.node, hNode.parent);
        }
    }

    private void unlockH(HNode hNode, QNode qNode) {
        if (hNode.parent == null) { // top hierarchy
            releaseHelper(hNode, qNode, UNLOCKED);
            return;
        }
        int curCount = qNode.status.get();
        if (curCount == 200_000) {
            unlockH(hNode.parent, hNode.node);
            releaseHelper(hNode, qNode, ACQUIRE_PARENT);
            return;
        }
        QNode succ = qNode.next.get();
        if (succ != null) {
            succ.status.set(curCount + 1);
            LockSupport.unpark(succ.thread.get());
            return;
        }
        unlockH(hNode.parent, hNode.node);
        releaseHelper(hNode, qNode, ACQUIRE_PARENT);
    }

    private void releaseHelper(HNode l, QNode i, int val) {
        QNode succ = i.next.get();
        i.thread.set(null);
        if (succ != null) {
            succ.status.set(val);
            LockSupport.unpark(succ.thread.get());
        } else {
            if (l.tail.compareAndSet(i, null)) {
                return;
            }
            do {
                succ = i.next.get();
            } while (succ == null);
            succ.status.set(val);
            LockSupport.unpark(succ.thread.get());
        }
    }
}
