package ru.ricnorr.numa.locks.hmcs_park;

import java.util.ArrayList;
import java.util.List;

import ru.ricnorr.numa.locks.Utils;

/**
 * Взять лок на CCL, затем на нума ноде, затем на супер-нума ноде, затем глобальный
 * На 48 корной машинке нет смысла считать, только на 96 и 128
 */
public class HMCSCclNumaSupernumaPark extends AbstractHMCSPark<HMCSQNodePark> {

  public HMCSCclNumaSupernumaPark() {
    super(HMCSQNodePark::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
    int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;
    var root = new HNode(null, new HMCSQNodePark());
    List<HNode> superNumaNodes = new ArrayList<>();
    for (int i = 0; i < Utils.NUMA_NODES_CNT / 2; i++) {
      superNumaNodes.add(new HNode(root, new HMCSQNodePark()));
    }
    List<HNode> numaNodes = new ArrayList<>();
    for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
      numaNodes.add(new HNode(superNumaNodes.get(i / 2), new HMCSQNodePark()));
    }
    for (int i = 0; i < Utils.CCL_CNT; i++) {
      leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new HMCSQNodePark());
    }
  }
}
