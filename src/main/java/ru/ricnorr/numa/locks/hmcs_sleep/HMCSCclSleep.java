package ru.ricnorr.numa.locks.hmcs_sleep;

import ru.ricnorr.numa.locks.Utils;


/**
 * HMCS, иерархия только на ccl'ях, то есть поток берет лок на своей ccl, затем глобальный лок.
 */
public class HMCSCclSleep extends AbstractHMCSSleep<HMCSQNodeSleepSleep> {

    public HMCSCclSleep() {
        super(HMCSQNodeSleepSleep::new, Utils::getKunpengCCLId, Utils.CCL_CNT, false);
        var root = new HNode(null, new HMCSQNodeSleepSleep());
        for (int i = 0; i < Utils.CCL_CNT; i++) {
            leafs[i] = new HNode(root, new HMCSQNodeSleepSleep());
        }
    }
}
