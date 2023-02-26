package ru.ricnorr.numa.locks.hmcs.pad;

import ru.ricnorr.numa.locks.Utils;

/**
 * HMCS, иерархия только на ccl'ях, то есть поток берет лок на своей ccl, затем глобальный лок.
 */
public class HmcsOnlyCclHierarchyPad extends AbstractHmcsPad {

    public HmcsOnlyCclHierarchyPad(boolean overSubscription, boolean isLight) {
        super(overSubscription, isLight, Utils::kungpengGetClusterID, Runtime.getRuntime().availableProcessors() / CCL_SIZE);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        var root = new HNode(null);
        for (int i = 0; i < availableProcessors / CCL_SIZE; i++) {
            leafs[i] = new HNode(root);
        }
    }
}
