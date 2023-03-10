package ru.ricnorr.numa.locks.cna_ccl_mcs_numa;

import jdk.internal.vm.annotation.Contended;
import kotlin.Pair;
import ru.ricnorr.numa.locks.NumaLock;
import ru.ricnorr.numa.locks.Utils;
import ru.ricnorr.numa.locks.cna.CNACclNoPad;
import ru.ricnorr.numa.locks.cna.CNANodeNoPad;

import java.util.concurrent.atomic.AtomicReference;

import static ru.ricnorr.numa.locks.cna_ccl_mcs_numa.CnaCclMcsNuma.QNode.LOCK_OWNER;
import static ru.ricnorr.numa.locks.cna_ccl_mcs_numa.CnaCclMcsNuma.QNode.NOT_OWNER;

/**
 * Аналог HMCS, берет лок на CNA_CCL на текущем нума узле. Затем берет HNode, и становимся в очередь на HMCS
 */
public class CnaCclMcsNuma implements NumaLock {

    final CNACclNoPad[] cnaArray = new CNACclNoPad[Utils.getNumaNodesCnt()];
    final QNode[] qnodeArray = new QNode[Utils.getNumaNodesCnt()];

    AtomicReference<QNode> mcsTailAtomRef = new AtomicReference<>(null);

    ThreadLocal<Integer> numaNodeThreadLocal = ThreadLocal.withInitial(Utils::getNumaNodeId);

    public CnaCclMcsNuma(boolean isLight) {
        for (int i = 0; i < cnaArray.length; i++) {
            cnaArray[i] = new CNACclNoPad();
            qnodeArray[i] = new QNode();
        }
    }

    @Override
    public Object lock() {
        var carrierThread = Utils.getCurrentCarrierThread();
        var numaNode = Utils.getByThreadFromThreadLocal(numaNodeThreadLocal, carrierThread);
        var cnaNode = cnaArray[numaNode].lock();

        var qnode = qnodeArray[numaNode];
        if (qnode.status == NOT_OWNER) {
            qnode.next = null;
            var mcsTail = mcsTailAtomRef.getAndSet(qnode);
            if (mcsTail != null) {
                mcsTail.next = qnode;
                while (qnode.status == NOT_OWNER) {
                    Thread.onSpinWait();
                }
            } else {
                qnode.status = LOCK_OWNER;
            }
        }
        return new Pair<>(numaNode, (CNANodeNoPad) cnaNode);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unlock(Object obj) {
        var p = (Pair<Integer, CNANodeNoPad>) obj;
        var numaNode = p.component1();
        var cnaNode = p.component2();
        var qnode = qnodeArray[numaNode];
        var cna = cnaArray[numaNode];
        var status = qnode.status;

        var hasNextInCna = cna.hasNext(cnaNode);


        if (status >= 10000 || !hasNextInCna) {
            if (qnode.next == null) {
                if (!mcsTailAtomRef.compareAndSet(qnode, null)) {
                    while (qnode.next == null) {
                        Thread.onSpinWait();
                    }
                    qnode.next.status = LOCK_OWNER;
                }
            } else {
                qnode.next.status = LOCK_OWNER;
            }
            qnode.status = NOT_OWNER;
        } else {
            qnode.status = status + 1;
        }

        cnaArray[numaNode].unlock(cnaNode);
    }

    @Contended
    public static class QNode {
        public static int NOT_OWNER = Integer.MAX_VALUE;
        public static int LOCK_OWNER = 0x0;

        private volatile QNode next = null;
        private volatile int status = NOT_OWNER;
    }
}
