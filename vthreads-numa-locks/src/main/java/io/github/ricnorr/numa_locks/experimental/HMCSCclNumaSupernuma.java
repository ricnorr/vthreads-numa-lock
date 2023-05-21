package io.github.ricnorr.numa_locks.experimental;

import java.util.ArrayList;
import java.util.List;

import io.github.ricnorr.numa_locks.LockUtils;

/**
 * Взять лок на CCL, затем на нума ноде, затем на супер-нума ноде, затем глобальный
 * На 48 корной машинке нет смысла считать, только на 96 и 128
 */
public class HMCSCclNumaSupernuma extends AbstractHMCS {

  public HMCSCclNumaSupernuma() {
    super(HMCSQNode::new, LockUtils::getKunpengCCLId, LockUtils.CCL_CNT);
    int cclPerNuma = LockUtils.CCL_CNT / LockUtils.NUMA_NODES_CNT;
    var root = new HNode(null, new HMCSQNode());
    List<HNode> superNumaNodes = new ArrayList<>();
    for (int i = 0; i < LockUtils.NUMA_NODES_CNT / 2; i++) {
      superNumaNodes.add(new HNode(root, new HMCSQNode()));
    }
    List<HNode> numaNodes = new ArrayList<>();
    for (int i = 0; i < LockUtils.NUMA_NODES_CNT; i++) {
      numaNodes.add(new HNode(superNumaNodes.get(i / 2), new HMCSQNode()));
    }
    for (int i = 0; i < LockUtils.CCL_CNT; i++) {
      leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new HMCSQNode());
    }
  }
}
