package ru.ricnorr.numa.locks.hmcs_park;

import ru.ricnorr.numa.locks.Utils;

public class HMCSNumaPark extends AbstractHMCSPark<HMCSQNodePark> {


  public HMCSNumaPark() {
    super(HMCSQNodePark::new, Utils::getNumaNodeId, Utils.NUMA_NODES_CNT);
    var root = new HNode(null, new HMCSQNodePark());
    for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
      leafs[i] = new HNode(root, new HMCSQNodePark());
    }
  }
}
