package ru.ricnorr.numa.locks.hmcs_sleep;

import ru.ricnorr.numa.locks.Utils;

public class HMCSNumaSleep extends AbstractHMCSSleep<HMCSQNodeSleepSleep> {


    public HMCSNumaSleep() {
        super(HMCSQNodeSleepSleep::new, Utils::getNumaNodeId, Utils.NUMA_NODES_CNT, false);
        var root = new HNode(null, new HMCSQNodeSleepSleep());
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            leafs[i] = new HNode(root, new HMCSQNodeSleepSleep());
        }
    }
}
