package io.github.ricnorr.numa_locks;

import java.util.ArrayList;
import java.util.List;

public class HMCSCclNuma extends AbstractHMCS {

  public HMCSCclNuma() {
    super(HMCSQNode::new, LockUtils::getKunpengCCLId, LockUtils.CCL_CNT);
    int cclPerNuma = LockUtils.CCL_CNT / LockUtils.NUMA_NODES_CNT;
    var root = new HNode(null, new HMCSQNode());
    List<HNode> numaNodes = new ArrayList<>();
    for (int i = 0; i < LockUtils.NUMA_NODES_CNT; i++) {
      numaNodes.add(new HNode(root, new HMCSQNode()));
    }
    for (int i = 0; i < LockUtils.CCL_CNT; i++) {
      leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new HMCSQNode());
    }
  }
}
