package ru.ricnorr.numa.locks.hmcs_park;

import ru.ricnorr.numa.locks.Utils;


/**
 * HMCS, иерархия только на ccl'ях, то есть поток берет лок на своей ccl, затем глобальный лок.
 */
public class HMCSCclPark extends AbstractHMCSPark<HMCSQNodePark> {

  public HMCSCclPark() {
    super(HMCSQNodePark::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
    var root = new HNode(null, new HMCSQNodePark());
    for (int i = 0; i < Utils.CCL_CNT; i++) {
      leafs[i] = new HNode(root, new HMCSQNodePark());
    }
  }
}
