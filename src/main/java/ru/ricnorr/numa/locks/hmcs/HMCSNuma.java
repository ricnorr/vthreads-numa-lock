package ru.ricnorr.numa.locks.hmcs;

import ru.ricnorr.numa.locks.Utils;

public class HMCSNuma extends AbstractHMCS<HMCSQNode> {


    public HMCSNuma() {
        super(HMCSQNode::new, Utils::getNumaNodeId, Utils.NUMA_NODES_CNT);
        var root = new HNode(null, new HMCSQNode());
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            leafs[i] = new HNode(root, new HMCSQNode());
        }
    }
}
