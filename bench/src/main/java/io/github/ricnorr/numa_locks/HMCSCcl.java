package io.github.ricnorr.numa_locks;

public class HMCSCcl extends AbstractHMCS {

  public HMCSCcl(boolean useFlag) {
    super(HMCSQNode::new, LockUtils::getKunpengCCLId, LockUtils.CCL_CNT, useFlag);
    var root = new HNode(null, new HMCSQNode());
    for (int i = 0; i < LockUtils.CCL_CNT; i++) {
      leafs[i] = new HNode(root, new HMCSQNode());
    }
  }
}
