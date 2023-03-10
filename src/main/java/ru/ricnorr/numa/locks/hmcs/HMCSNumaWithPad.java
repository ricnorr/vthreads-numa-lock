package ru.ricnorr.numa.locks.hmcs;

import ru.ricnorr.numa.locks.Utils;

public class HMCSNumaWithPad extends AbstractHMCS<HMCSQNodeWithPad> {


    public HMCSNumaWithPad() {
        super(HMCSQNodeWithPad::new, Utils::getNumaNodeId, Utils.NUMA_NODES_CNT);
        var root = new HNode(null, new HMCSQNodeWithPad());
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            leafs[i] = new HNode(root, new HMCSQNodeWithPad());
        }
    }
}
