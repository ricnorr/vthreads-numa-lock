package ru.ricnorr.numa.locks.effective_hmcs;

import java.util.ArrayList;
import java.util.List;

import ru.ricnorr.numa.locks.Utils;

/**
 * Effective adaptation of HMCS lock for VirtualThread for Kunpeng-920 servers
 * <a href="https://www.hisilicon.com/en/products/Kunpeng/Huawei-Kunpeng/Huawei-Kunpeng-920">...</a>
 */
public class HMCSEffective extends EffectiveAbstractHMCS {

  public HMCSEffective() {
    super(QNode::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
    int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;
    if (Runtime.getRuntime().availableProcessors() == 128 || Runtime.getRuntime().availableProcessors() == 96) {
      // on servers with CCL+NUMA
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
      // on servers with CCL+NUMA
      root = new HNode(null, new QNode());
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
