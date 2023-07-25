package io.github.ricnorr.numa_locks;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import jdk.internal.vm.annotation.Contended;

@Contended
public class VNA_2_Q implements VthreadNumaLock<VNA_2_Q.UnlockInfo> {
    private static final VarHandle VALUE;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(VNA_2_Q.class, "globalLock", Boolean.TYPE);
        } catch (ReflectiveOperationException var1) {
            throw new ExceptionInInitializerError(var1);
        }
    }

    final List<AtomicReference<Node>> localQueues;
    ThreadLocal<Integer> numaNodeThreadLocal = ThreadLocal.withInitial(LockUtils::getNumaNodeId);
    ThreadLocal<Integer> lockAcquiresThreadLocal = ThreadLocal.withInitial(() -> 0);
    volatile boolean globalLock = false;

    /**
     * Creates NUMA_MCS lock.
     */
    public VNA_2_Q() {
        this.localQueues = new ArrayList<>();
        for (int i = 0; i < LockUtils.NUMA_NODES_CNT; i++) {
            localQueues.add(new AtomicReference<>());
        }
    }


    private boolean casGlobalLock(boolean expected, boolean newValue) {
        return VALUE.compareAndSet(this, expected, newValue);
    }

    @Override
    public UnlockInfo lock() {
        var node = new Node();
        var superNumaId = LockUtils.getByThreadFromThreadLocal(numaNodeThreadLocal,
                LockUtils.getCurrentCarrierThread()) / 2;
        var lockAcquires =
                LockUtils.getByThreadFromThreadLocal(lockAcquiresThreadLocal, LockUtils.getCurrentCarrierThread());
        lockAcquires++;
        if (lockAcquires >= 10_000) {
            lockAcquires = 1;
            LockUtils.setByThreadToThreadLocal(numaNodeThreadLocal, LockUtils.getCurrentCarrierThread(),
                    LockUtils.getNumaNodeId());
        }
        LockUtils.setByThreadToThreadLocal(lockAcquiresThreadLocal, LockUtils.getCurrentCarrierThread(), lockAcquires);

        if (casGlobalLock(false, true)) {
            return new UnlockInfo(true, superNumaId, node);
        }
        var localQueue = localQueues.get(superNumaId);
        var pred = localQueue.getAndSet(node);
        if (pred == null) {
            while (!casGlobalLock(false, true)) {
                Thread.onSpinWait();
            }
            return new UnlockInfo(false, superNumaId, node);
        }
        pred.next.set(node);
        int iterations = 0;
        while (node.spin) {
            iterations++;
            if (iterations == 1024) {
                LockSupport.park();
                iterations = 0;
            }
        }
        while (!casGlobalLock(false, true)) {
            Thread.onSpinWait();
        }
        return new UnlockInfo(false, superNumaId, node);
    }

    @Override
    public void unlock(UnlockInfo unlockInfo) {
        globalLock = false;
        if (unlockInfo.fastPath) {
            return;
        }
        var localQueue = localQueues.get(unlockInfo.numaId);
        if (unlockInfo.node.next.get() == null) {
            if (localQueue.compareAndSet(unlockInfo.node, null)) {
                return;
            }
            while (unlockInfo.node.next.get() == null) {

            }
        }
        unlockInfo.node.next.get().spin = false;
        LockSupport.unpark(unlockInfo.node.next.get().thread);
    }

    record UnlockInfo(
            boolean fastPath,
            int numaId,

            Node node
    ) {
    }

    @Contended
    private static class Node {

        Thread thread = Thread.currentThread();

        volatile boolean spin = true;

        AtomicReference<Node> next = new AtomicReference<>();

    }
}
