package ru.ricnorr.numa.locks.hmcs_park;

import java.util.ArrayList;
import java.util.List;

import ru.ricnorr.numa.locks.Utils;

public class HMCSCclNumaPark extends AbstractHMCSPark<HMCSQNodePark> {

  public HMCSCclNumaPark() {
    super(HMCSQNodePark::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
    int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;
    var root = new HNode(null, new HMCSQNodePark());
    List<HNode> numaNodes = new ArrayList<>();
    for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
      numaNodes.add(new HNode(root, new HMCSQNodePark()));
    }
    for (int i = 0; i < Utils.CCL_CNT; i++) {
      leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new HMCSQNodePark());
    }
  }
}
