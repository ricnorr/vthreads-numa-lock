package ru.ricnorr.numa.locks.hmcs_exp;

import java.util.ArrayList;
import java.util.List;

import ru.ricnorr.numa.locks.Utils;

public class HMCSCclNumaExpv2 extends AbstractHMCSExpv2 {

  public HMCSCclNumaExpv2() {
    super(HMCSQNodeExp::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
    int cclPerNuma = Utils.CCL_CNT / Utils.NUMA_NODES_CNT;
    root = new HNode(null, new HMCSQNodeExp());
    List<HNode> numaNodes = new ArrayList<>();
    for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
      numaNodes.add(new HNode(root, new HMCSQNodeExp()));
    }
    for (int i = 0; i < Utils.CCL_CNT; i++) {
      leafs[i] = new HNode(numaNodes.get(i / cclPerNuma), new HMCSQNodeExp());
    }
  }
}
