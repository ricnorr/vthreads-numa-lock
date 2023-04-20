package ru.ricnorr.numa.locks.hmcs_exp;

import ru.ricnorr.numa.locks.Utils;

public class HMCSNuma extends AbstractHMCSExpv2 {


  public HMCSNuma() {
    super(HMCSQNodeExp::new, Utils::getNumaNodeId, Utils.NUMA_NODES_CNT);
    root = new HNode(null, new HMCSQNodeExp());
    for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
      leafs[i] = new HNode(root, new HMCSQNodeExp());
    }
  }
}
