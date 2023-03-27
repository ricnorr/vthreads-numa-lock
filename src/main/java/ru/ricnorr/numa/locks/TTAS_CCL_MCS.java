package ru.ricnorr.numa.locks;

import kotlin.Pair;
import ru.ricnorr.numa.locks.basic.MCS;
import ru.ricnorr.numa.locks.basic.MCS_SLEEP;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;


public class TTAS_CCL_MCS extends AbstractNumaLock {

    final AtomicReferenceArray<Boolean> atomicBooleans;

    final MCS mcs;

    public TTAS_CCL_MCS() {
        super(Utils::getKunpengCCLId);
        atomicBooleans = new AtomicReferenceArray<>(Utils.CCL_CNT);
        for (int i = 0; i < Utils.CCL_CNT; i++) {
            atomicBooleans.set(i, false);
        }
        mcs = new MCS();
    }


    @Override
    public Object lock(Object obj) {
        while (true) {
            int kunpengCclId = getClusterId();
            boolean ttasLocked = false;
            for (int i = 0; i < 256; i++) {
                if (atomicBooleans.compareAndSet(kunpengCclId, false, true)) {
                    ttasLocked = true;
                    break;
                }
            }
            if (ttasLocked) {
                return new Pair<>(mcs.lock(null), kunpengCclId);
            } else {
                Thread.yield();
            }
        }
    }

    @Override
    public void unlock(Object obj) {
        var pair = (Pair<Object, Integer>) obj;
        mcs.unlock(pair.component1());
        atomicBooleans.set(pair.component2(), false);
    }

    @Override
    public boolean hasNext(Object obj) {
        return false;
    }
}
