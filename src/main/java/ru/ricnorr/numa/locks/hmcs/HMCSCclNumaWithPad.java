package ru.ricnorr.numa.locks.hmcs;

import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;

public class HMCSCclNumaWithPad extends AbstractHMCS<HMCSQNodeWithPad> {

    public HMCSCclNumaWithPad() {
        super(HMCSQNodeWithPad::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
        int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;
        var root = new HNode(null, new HMCSQNodeWithPad());
        List<HNode> numaNodes = new ArrayList<>();
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            numaNodes.add(new HNode(root, new HMCSQNodeWithPad()));
        }
        for (int i = 0; i < Utils.CCL_CNT; i++) {
            leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new HMCSQNodeWithPad());
        }
    }
}
