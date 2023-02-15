package ru.ricnorr.numa.locks;


public class HmcsOnlyNumaHierarchy extends AbstractHmcs {


    public HmcsOnlyNumaHierarchy(boolean overSubscription, boolean isLight) {
        super(overSubscription, isLight, Utils::getClusterID);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int numaNodesCount;
        if (availableProcessors == 96 || availableProcessors == 128) {
            numaNodesCount = 4;
        } else {
            numaNodesCount = 2;
        }
        var root = new HNode(null);
        for (int i = 0; i < numaNodesCount; i++) {
            leafs.add(new HNode(root));
        }
    }
}
