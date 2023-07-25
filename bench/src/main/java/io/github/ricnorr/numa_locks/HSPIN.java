package io.github.ricnorr.numa_locks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HSPIN implements VthreadNumaLock<HSPIN.HSPINInfo> {
    private List<AtomicBoolean> numaSpinLocks = new ArrayList<>();

    private final AtomicBoolean globalLock = new AtomicBoolean(false);
    private final boolean useFastPath;


    ThreadLocal<Integer> numaNodeThreadLocal = ThreadLocal.withInitial(LockUtils::getNumaNodeId);

    ThreadLocal<Integer> lockAcquiresThreadLocal = ThreadLocal.withInitial(() -> 0);


    public HSPIN(boolean useFastPath) {
        this.numaSpinLocks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            numaSpinLocks.add(new AtomicBoolean(false));
        }
        this.useFastPath = useFastPath;
    }

    record HSPINInfo(
            int numaId,
            boolean fastPath
    ) {

    }

    @Override
    public HSPINInfo lock() {
        var numaId = LockUtils.getByThreadFromThreadLocal(numaNodeThreadLocal, LockUtils.getCurrentCarrierThread());
        var lockAcquires =
                LockUtils.getByThreadFromThreadLocal(lockAcquiresThreadLocal, LockUtils.getCurrentCarrierThread());
        lockAcquires++;
        if (lockAcquires >= 10_000) {
            lockAcquires = 1;
            LockUtils.setByThreadToThreadLocal(numaNodeThreadLocal, LockUtils.getCurrentCarrierThread(),
                    LockUtils.getNumaNodeId());
        }
        LockUtils.setByThreadToThreadLocal(lockAcquiresThreadLocal, LockUtils.getCurrentCarrierThread(), lockAcquires);
        if (useFastPath) {
            if (globalLock.compareAndSet(false, true)) {
                return new HSPINInfo(numaId, true);
            }
        }
        int i = 0;
        while (!numaSpinLocks.get(numaId).compareAndSet(false, true)) {
            i++;
            if (i == 30) {
                i = 0;
                Thread.yield();
            }
        }
        i = 0;
        while (!globalLock.compareAndSet(false, true)) {
            i++;
            if (i == 30) {
                i = 0;
                Thread.yield();
            }
        }
        return new HSPINInfo(numaId, false);
    }

    @Override
    public void unlock(HSPINInfo unlockInfo) {
        if (!unlockInfo.fastPath) {
            numaSpinLocks.get(unlockInfo.numaId).set(false);
        }
        globalLock.set(false);
    }
}
