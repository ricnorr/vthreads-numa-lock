package ru.ricnorr.numa.locks.hmcs.pad;


import ru.ricnorr.numa.locks.Utils;

public class HmcsOnlyNumaHierarchyPad extends AbstractHmcsPad {


    public HmcsOnlyNumaHierarchyPad(boolean overSubscription, boolean isLight) {
        super(overSubscription, isLight, Utils::getClusterID, getNumaNodesCnt());
        var root = new HNode(null);
        for (int i = 0; i < getNumaNodesCnt(); i++) {
            leafs[i] = new HNode(root);
        }
    }

    private static int getNumaNodesCnt() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (availableProcessors == 96 || availableProcessors == 128) {
            return 4;
        } else {
            return 2;
        }
    }
}
