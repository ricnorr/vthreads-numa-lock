package io.github.ricnorr.numa_locks;

public class HMCSNuma extends AbstractHMCS {


  public HMCSNuma() {
    super(HMCSQNode::new, LockUtils::getNumaNodeId, LockUtils.NUMA_NODES_CNT);
    var root = new HNode(null, new HMCSQNode());
    for (int i = 0; i < LockUtils.NUMA_NODES_CNT; i++) {
      leafs[i] = new HNode(root, new HMCSQNode());
    }
  }
}
