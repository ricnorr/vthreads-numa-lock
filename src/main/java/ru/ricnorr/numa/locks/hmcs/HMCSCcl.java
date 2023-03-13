package ru.ricnorr.numa.locks.hmcs;

import ru.ricnorr.numa.locks.Utils;


/**
 * HMCS, иерархия только на ccl'ях, то есть поток берет лок на своей ccl, затем глобальный лок.
 */
public class HMCSCcl extends AbstractHMCS<HMCSQNode> {

    public HMCSCcl() {
        super(HMCSQNode::new, Utils::getKunpengCCLId, Utils.CCL_CNT);
        var root = new HNode(null, new HMCSQNode());
        for (int i = 0; i < Utils.CCL_CNT; i++) {
            leafs[i] = new HNode(root, new HMCSQNode());
        }
    }
}
