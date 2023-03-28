package ru.ricnorr.numa.locks.hmcs_sleep;

import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;

public class HMCSCclNumaSleep extends AbstractHMCSSleep<HMCSQNodeSleepSleep> {

    public HMCSCclNumaSleep(boolean oversubscription) {
        super(HMCSQNodeSleepSleep::new, Utils::getKunpengCCLId, Utils.CCL_CNT, oversubscription);
        int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;
        var root = new HNode(null, new HMCSQNodeSleepSleep());
        List<HNode> numaNodes = new ArrayList<>();
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            numaNodes.add(new HNode(root, new HMCSQNodeSleepSleep()));
        }
        for (int i = 0; i < Utils.CCL_CNT; i++) {
            leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new HMCSQNodeSleepSleep());
        }
    }
}
