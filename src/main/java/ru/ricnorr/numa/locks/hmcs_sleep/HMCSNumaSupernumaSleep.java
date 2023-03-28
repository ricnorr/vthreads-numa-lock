package ru.ricnorr.numa.locks.hmcs_sleep;

import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;

public class HMCSNumaSupernumaSleep extends AbstractHMCSSleep<HMCSQNodeSleepSleep> {


    public HMCSNumaSupernumaSleep() {
        super(HMCSQNodeSleepSleep::new, Utils::getNumaNodeId, Utils.NUMA_NODES_CNT, false);
        int superNumaCnt = Utils.NUMA_NODES_CNT / 2;
        var root = new HNode(null, new HMCSQNodeSleepSleep());
        List<HNode> superNumaNodes = new ArrayList<>();
        for (int i = 0; i < superNumaCnt; i++) {
            superNumaNodes.add(new HNode(root, new HMCSQNodeSleepSleep()));
        }
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            leafs[i] = new HNode(superNumaNodes.get(i / superNumaCnt), new HMCSQNodeSleepSleep());
        }
    }
}
