package ru.ricnorr.numa.locks.hmcs_park;

import java.util.ArrayList;
import java.util.List;

import ru.ricnorr.numa.locks.Utils;

public class HMCSNumaSupernumaPark extends AbstractHMCSPark<HMCSQNodePark> {


  public HMCSNumaSupernumaPark() {
    super(HMCSQNodePark::new, Utils::getNumaNodeId, Utils.NUMA_NODES_CNT);
    int superNumaCnt = Utils.NUMA_NODES_CNT / 2;
    var root = new HNode(null, new HMCSQNodePark());
    List<HNode> superNumaNodes = new ArrayList<>();
    for (int i = 0; i < superNumaCnt; i++) {
      superNumaNodes.add(new HNode(root, new HMCSQNodePark()));
    }
    for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
      leafs[i] = new HNode(superNumaNodes.get(i / superNumaCnt), new HMCSQNodePark());
    }
  }
}
