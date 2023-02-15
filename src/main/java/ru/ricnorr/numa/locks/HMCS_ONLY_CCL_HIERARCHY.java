package ru.ricnorr.numa.locks;

/**
 * HMCS, иерархия только на ccl'ях, то есть поток берет лок на своей ccl, затем глобальный лок.
 */
public class HMCS_ONLY_CCL_HIERARCHY extends AbstractHmcs {

    public HMCS_ONLY_CCL_HIERARCHY(boolean overSubscription, boolean isLight) {
        super(overSubscription, isLight, Utils::kungpengGetClusterID);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int cclSize = 4;
        var root = new HNode(null);
        for (int i = 0; i < availableProcessors / cclSize; i++) {
            leafs.add(new HNode(root));
        }
    }
}
