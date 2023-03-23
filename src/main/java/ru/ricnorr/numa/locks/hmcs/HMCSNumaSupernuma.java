package ru.ricnorr.numa.locks.hmcs;

import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;

public class HMCSNumaSupernuma extends AbstractHMCS<HMCSQNode> {


    public HMCSNumaSupernuma() {
        super(HMCSQNode::new, Utils::getNumaNodeId, Utils.NUMA_NODES_CNT);
        int superNumaCnt = Utils.NUMA_NODES_CNT / 2;
        var root = new HNode(null, new HMCSQNode());
        List<HNode> superNumaNodes = new ArrayList<>();
        for (int i = 0; i < superNumaCnt; i++) {
            superNumaNodes.add(new HNode(root, new HMCSQNode()));
        }
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            leafs[i] = new HNode(superNumaNodes.get(i / superNumaCnt), new HMCSQNode());
        }
    }
}
