package ru.ricnorr.numa.locks.hmcs;


import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Сначала берем лок на CCL, затем берем лок на нума ноде, затем берем глобальный
 */
public class HMCSCclNumaNoPad extends AbstractHMCS<HMCSQNodeNoPad> {

    public HMCSCclNumaNoPad() {
        super(HMCSQNodeNoPad::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
        int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;
        var root = new HNode(null, new HMCSQNodeNoPad());
        List<HNode> numaNodes = new ArrayList<>();
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            numaNodes.add(new HNode(root, new HMCSQNodeNoPad()));
        }
        for (int i = 0; i < Utils.CCL_CNT; i++) {
            leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new HMCSQNodeNoPad());
        }
    }
}
