package ru.ricnorr.numa.locks.hmcs_exp;

import ru.ricnorr.numa.locks.Utils;


/**
 * HMCS, иерархия только на ccl'ях, то есть поток берет лок на своей ccl, затем глобальный лок.
 */
public class HMCSCcl extends AbstractHMCSExpv2 {

  public HMCSCcl() {
    super(HMCSQNodeExp::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
    root = new HNode(null, new HMCSQNodeExp());
    for (int i = 0; i < Utils.CCL_CNT; i++) {
      leafs[i] = new HNode(root, new HMCSQNodeExp());
    }
  }
}
