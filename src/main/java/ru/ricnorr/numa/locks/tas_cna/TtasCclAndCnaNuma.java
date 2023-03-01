package ru.ricnorr.numa.locks.tas_cna;

import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;
import ru.ricnorr.numa.locks.basic.TestTestAndSetLock;
import ru.ricnorr.numa.locks.cna.nopad.CNANode;
import ru.ricnorr.numa.locks.cna.nopad.CnaNuma;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Комбинация TTAS на уровне CCL и CNA на уровне NUMA
 */
public class TtasCclAndCnaNuma implements NumaLock {

    final TestTestAndSetLock[] ttasLocksForCcl;

    final CnaNuma cnaNuma;

    final boolean isLight;

    ThreadLocal<Integer> cclIdThreadLocal = ThreadLocal.withInitial(Utils::kungpengGetClusterID);

    public TtasCclAndCnaNuma(boolean useLightThreads) {
        ttasLocksForCcl = new TestTestAndSetLock[Runtime.getRuntime().availableProcessors() / 4];
        isLight = useLightThreads;
        for (int i = 0; i < ttasLocksForCcl.length; i++) {
            ttasLocksForCcl[i] = new TestTestAndSetLock();
        }
        cnaNuma = new CnaNuma(useLightThreads);
    }

    @Override
    public Object lock() {
        var carrierThread = Utils.getCurrentCarrierThread();
        if (ThreadLocalRandom.current().nextInt() % 5431 == 0) {
            Utils.setByThreadToThreadLocal(cclIdThreadLocal, carrierThread, Utils.kungpengGetClusterID());
        }
        var cclId = Utils.getByThreadFromThreadLocal(cclIdThreadLocal, carrierThread);
        ttasLocksForCcl[cclId].lock();
        var node = cnaNuma.lock();
        return new Pair((CNANode) node, cclId);
    }

    @Override
    public void unlock(Object obj) {
        var pair = (Pair) obj;
        ttasLocksForCcl[pair.cclId].unlock(null);
        cnaNuma.unlock(pair.cnaNode);
    }

    private class Pair {
        final CNANode cnaNode;

        final int cclId;

        public Pair(CNANode cnaNode, int cclId) {
            this.cnaNode = cnaNode;
            this.cclId = cclId;
        }
    }
}
