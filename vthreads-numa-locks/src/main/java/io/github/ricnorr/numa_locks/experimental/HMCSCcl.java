package io.github.ricnorr.numa_locks.experimental;

import io.github.ricnorr.numa_locks.LockUtils;

public class HMCSCcl extends AbstractHMCS {

  public HMCSCcl() {
    super(HMCSQNode::new, LockUtils::getKunpengCCLId, LockUtils.CCL_CNT);
    var root = new HNode(null, new HMCSQNode());
    for (int i = 0; i < LockUtils.CCL_CNT; i++) {
      leafs[i] = new HNode(root, new HMCSQNode());
    }
  }
}
