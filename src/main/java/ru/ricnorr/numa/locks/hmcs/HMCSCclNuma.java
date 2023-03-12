package ru.ricnorr.numa.locks.hmcs;

import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;

public class HMCSCclNuma extends AbstractHMCS<HMCSQNode> {

    public HMCSCclNuma() {
        super(HMCSQNode::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
        int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;
        var root = new HNode(null, new HMCSQNode());
        List<HNode> numaNodes = new ArrayList<>();
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            numaNodes.add(new HNode(root, new HMCSQNode()));
        }
        for (int i = 0; i < Utils.CCL_CNT; i++) {
            leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new HMCSQNode());
        }
    }
}
