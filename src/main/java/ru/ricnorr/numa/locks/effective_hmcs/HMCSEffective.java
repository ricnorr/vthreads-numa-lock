package ru.ricnorr.numa.locks.hmcs_exp;

import java.util.ArrayList;
import java.util.List;

import ru.ricnorr.numa.locks.Utils;

public class HMCSCclNumaEffective extends EffectiveAbstractHMCS {

  public HMCSCclNumaEffective() {
    super(QNode::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
    int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;
    if (Runtime.getRuntime().availableProcessors() == 128 || Runtime.getRuntime().availableProcessors() == 96) {
      root = new HNode(null, new QNode());
      List<HNode> superNumaNodes = new ArrayList<>();
      for (int i = 0; i < Utils.NUMA_NODES_CNT / 2; i++) {
        superNumaNodes.add(new HNode(root, new QNode()));
      }
      List<HNode> numaNodes = new ArrayList<>();
      for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
        numaNodes.add(new HNode(superNumaNodes.get(i / 2), new QNode()));
      }
      for (int i = 0; i < Utils.CCL_CNT; i++) {
        leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new QNode());
      }
    } else {
      var root = new HNode(null, new QNode());
      List<HNode> numaNodes = new ArrayList<>();
      for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
        numaNodes.add(new HNode(root, new QNode()));
      }
      for (int i = 0; i < Utils.CCL_CNT; i++) {
        leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new QNode());
      }
    }
  }
}
