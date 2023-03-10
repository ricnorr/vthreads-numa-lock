//package ru.ricnorr.numa.locks.combination;
//
//import ru.ricnorr.benchmarks.LockType;
//import ru.ricnorr.numa.locks.NumaLock;
//import ru.ricnorr.numa.locks.Utils;
//import ru.ricnorr.numa.locks.hmcs.nopad.
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.concurrent.locks.LockSupport;
//import java.util.function.Supplier;
//
//import static ru.ricnorr.benchmarks.Main.initLock;
//
//public class CombinationLock {
//    public static final int CCL_SIZE = 4;
//    final HNode[] leafs;
//
//    final boolean overSubscription;
//
//    final boolean isLight;
//
//
//    final ThreadLocal<Integer> lastLevelChooseId;
//
//    public CombinationLock(boolean overSubscription, boolean isLight, List<CombinationLockLevelDescription> levelToDescription, Supplier<Integer> lastLevelChooseNode) {
//        this.isLight = isLight;
//        this.overSubscription = overSubscription;
//        CombinationLockLevelDescription prevDescription = levelToDescription.get(0);
//        List<HNode> nodesFromPrevLvl = List.of(new HNode(null, initLock(prevDescription.lockType, "", overSubscription, isLight)));
//        for (int i = 0; i < levelToDescription.size() - 1; i++) {
//            CombinationLockLevelDescription nextDescription = levelToDescription.get(i + 1);
//            List<HNode> curLvl = new ArrayList<>();
//            for (HNode nodeFromPrevLvl : nodesFromPrevLvl) {
//                for (int j = 0; j < prevDescription.childsOnNextLevel; j++) {
//                    curLvl.add(new HNode(nodeFromPrevLvl, initLock(nextDescription.lockType, "", overSubscription, isLight)));
//                }
//            }
//            nodesFromPrevLvl = curLvl;
//        }
//        leafs = nodesFromPrevLvl.toArray(new HNode[0]);
//        this.lastLevelChooseId = ThreadLocal.withInitial(lastLevelChooseNode);
//    }
//
//    @Override
//    public Object lock() {
//        var carrierThread = Utils.getCurrentCarrierThread();
//        var numaNode = Utils.getByThreadFromThreadLocal(lastLevelChooseId, carrierThread);
//        lockH(leafs[numaNode]);
//    }
//
//    @Override
//    public void unlock(Object obj) {
//        if (isLight) {
//            unlockH(leafs[Utils.getByThreadFromThreadLocal(carrierClusterId, Utils.getCurrentCarrierThread())], (QNode) obj);
//        } else {
//            unlockH(leafs[carrierClusterId.get()], localQNode.get());
//        }
//    }
//
//    private void lockH(HNode hNode, List<Object> objectsFromLocks) {
//        if (hNode.parent == null) {
////            qNode.next = null;
////            if (overSubscription) {
////                qNode.thread = Thread.currentThread();
////            }
////            qNode.status = LOCKED;
////            QNode pred = hNode.tail.getAndSet(qNode);
////            if (pred == null) {
////                qNode.status = UNLOCKED;
////            } else {
////                pred.next = qNode;
////                while (qNode.status == LOCKED) {
////                    if (overSubscription) {
////                        LockSupport.park();
////                    } else {
////                        Thread.onSpinWait();
////                    }
////                } // spin
////            }
//        } else {
//            var obj = hNode.numaLock.lock();
//            qNode.next = null;
//            if (overSubscription) {
//                qNode.thread = Thread.currentThread();
//            }
//            qNode.status = WAIT;
//            QNode pred = hNode.tail.getAndSet(qNode);
//            if (pred != null) {
//                pred.next = qNode;
//                while (qNode.status == WAIT) {
//                    if (overSubscription) {
//                        LockSupport.park(this);
//                    } else {
//                        Thread.onSpinWait();
//                    }
//                } // spin
//                if (qNode.status < ACQUIRE_PARENT) {
//                    return;
//                }
//            }
//            qNode.status = COHORT_START;
//            lockH(hNode.node, hNode.parent);
//        }
//    }
//
//    private void unlockH(HNode hNode, QNode qNode) {
//        if (hNode.parent == null) { // top hierarchy
//            releaseHelper(hNode, qNode, UNLOCKED);
//            return;
//        }
//        int curCount = qNode.status;
//        if (curCount == 10000) {
//            unlockH(hNode.parent, hNode.node);
//            releaseHelper(hNode, qNode, ACQUIRE_PARENT);
//            return;
//        }
//        QNode succ = qNode.next;
//        if (succ != null) {
//            succ.status = curCount + 1;
//            if (overSubscription) {
//                LockSupport.unpark(succ.thread);
//            }
//            return;
//        }
//        unlockH(hNode.parent, hNode.node);
//        releaseHelper(hNode, qNode, ACQUIRE_PARENT);
//    }
//
//    private void releaseHelper(HNode l, QNode i, int val) {
//        QNode succ = i.next;
//        if (succ != null) {
//            succ.status = val;
//            if (overSubscription) {
//                LockSupport.unpark(succ.thread);
//            }
//        } else {
//            if (l.tail.compareAndSet(i, null)) {
//                return;
//            }
//            do {
//                succ = i.next;
//            } while (succ == null);
//            succ.status = val;
//            if (overSubscription) {
//                LockSupport.unpark(succ.thread);
//            }
//        }
//    }
//
//    public static class HNode {
//        private final NumaLock numaLock;
//        private final HNode parent;
//        QNode node;
//
//        public HNode(HNode parent, NumaLock numaLock) {
//            this.parent = parent;
//            this.numaLock = numaLock;
//            this.node = new QNode();
//        }
//    }
//
//    public static class QNode {
//        public static int WAIT = Integer.MAX_VALUE;
//        public static int ACQUIRE_PARENT = Integer.MAX_VALUE - 1;
//        public static int UNLOCKED = 0x0;
//        public static int LOCKED = 0x1;
//        public static int COHORT_START = 0x1;
//
//        private volatile QNode next = null;
//        private volatile int status = WAIT;
//
//        private volatile Thread thread = null;
//
//    }
//
//    public class CombinationLockLevelDescription {
//        public LockType lockType;
//
//        public Integer childsOnNextLevel;
//    }
//}
