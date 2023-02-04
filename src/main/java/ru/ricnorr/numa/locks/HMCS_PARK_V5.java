package ru.ricnorr.numa.locks;

import kotlinx.atomicfu.AtomicRef;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static kotlinx.atomicfu.AtomicFU.atomic;
import static ru.ricnorr.numa.locks.HMCS_PARK_V5.QNode.*;

public class HMCS_PARK_V5 extends AbstractLock {
    public static class HNode {

        HNode parent;

        final AtomicRef<QNode> tail;

        QNode node;

        public HNode(HNode parent) {
            this.parent = parent;
            this.tail = atomic(null);
            this.node = new QNode();
        }
    }

    public static class QNode {

        static int WAIT = Integer.MAX_VALUE;
        static int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
        static int UNLOCKED = 0x0;
        static int LOCKED = 0x1;
        static int COHORT_START = 0x1;

        private volatile QNode next = null;
        private volatile int status = WAIT;

        private volatile Thread thread = null;

    }

    final List<HNode> leafs;

    final ThreadLocal<QNode> localQNode = ThreadLocal.withInitial(QNode::new);

    final ThreadLocal<Integer> localClusterID = ThreadLocal.withInitial(Utils::kungpengGetClusterID);

    public HMCS_PARK_V5(HMCSLockSpec spec) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int treeHeight = 4;
        int cclSize = (int)spec.ccl;
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
            qNode.next = null;
            qNode.thread = Thread.currentThread();
            qNode.status = LOCKED;
            QNode pred = hNode.tail.getAndSet(qNode);
            if (pred == null) {
                qNode.status = UNLOCKED;
            } else {
                pred.next = qNode;
                int counter = 0;
                while (qNode.status == LOCKED) {
                    if (counter < 256) {
                        Thread.onSpinWait();
                        counter++;
                    } else {
                        LockSupport.park(this);
                    }
                } // spin
                return;
            }
        } else {
            qNode.next = null;
            qNode.thread = Thread.currentThread();
            qNode.status = WAIT;
            QNode pred = hNode.tail.getAndSet(qNode);
            if (pred != null) {
                pred.next = qNode;
                int counter = 0;
                while (qNode.status == WAIT) {
                    if (counter < 256) {
                        Thread.onSpinWait();
                        counter++;
                    } else {
                        LockSupport.park(this);
                    }
                } // spin
                if (qNode.status < ACQUIRE_PARENT) {
                    return;
                }
            }
            qNode.status = COHORT_START;
            lockH(hNode.node, hNode.parent);
        }
    }

    private void unlockH(HNode hNode, QNode qNode) {
        if (hNode.parent == null) { // top hierarchy
            releaseHelper(hNode, qNode, UNLOCKED);
            return;
        }
        int curCount = qNode.status;
        if (curCount == 200_000) {
            unlockH(hNode.parent, hNode.node);
            releaseHelper(hNode, qNode, ACQUIRE_PARENT);
            return;
        }
        QNode succ = qNode.next;
        if (succ != null) {
            succ.status = curCount + 1;
            LockSupport.unpark(succ.thread);
            return;
        }
        unlockH(hNode.parent, hNode.node);
        releaseHelper(hNode, qNode, ACQUIRE_PARENT);
    }

    private void releaseHelper(HNode l, QNode i, int val) {
        QNode succ = i.next;
        if (succ != null) {
            succ.status = val;
            LockSupport.unpark(succ.thread);
        } else {
            if (l.tail.compareAndSet(i, null)) {
                return;
            }
            do {
                succ = i.next;
            } while (succ == null);
            succ.status = val;
            LockSupport.unpark(succ.thread);
        }
    }
}
