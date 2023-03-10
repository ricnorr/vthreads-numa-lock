package ru.ricnorr.numa.locks.hmcs.nopad;

import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Взять лок на CCL, затем на нума ноде, затем на супер-нума ноде, затем глобальный
 * На 48 корной машинке нет смысла считать, только на 96 и 128
 */
public class HMCSCclNumaSupernumaNopad extends AbstractHMCS<HMCSQNodeNoPad> {

    public HMCSCclNumaSupernumaNopad() {
        super(HMCSQNodeNoPad::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
        int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;
        var root = new HNode(null, new HMCSQNodeNoPad());
        List<HNode> superNumaNodes = new ArrayList<>();
        for (int i = 0; i < Utils.NUMA_NODES_CNT / 2; i++) {
            superNumaNodes.add(new HNode(root, new HMCSQNodeNoPad()));
        }
        List<HNode> numaNodes = new ArrayList<>();
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            numaNodes.add(new HNode(superNumaNodes.get(i / 2), new HMCSQNodeNoPad()));
        }
        for (int i = 0; i < Utils.CCL_CNT; i++) {
            leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new HMCSQNodeNoPad());
        }
    }
}
