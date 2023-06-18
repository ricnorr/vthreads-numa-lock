package io.github.ricnorr.numa_locks;

import java.util.ArrayList;
import java.util.List;

public class HMCSNumaSupernuma extends AbstractHMCS {


  public HMCSNumaSupernuma(boolean useFlag) {
    super(HMCSQNode::new, LockUtils::getNumaNodeId, LockUtils.NUMA_NODES_CNT, useFlag);
    int superNumaCnt = LockUtils.NUMA_NODES_CNT / 2;
    var root = new HNode(null, new HMCSQNode());
    List<HNode> superNumaNodes = new ArrayList<>();
    for (int i = 0; i < superNumaCnt; i++) {
      superNumaNodes.add(new HNode(root, new HMCSQNode()));
    }
    for (int i = 0; i < LockUtils.NUMA_NODES_CNT; i++) {
      leafs[i] = new HNode(superNumaNodes.get(i / superNumaCnt), new HMCSQNode());
    }
  }
}
