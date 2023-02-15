package ru.ricnorr.numa.locks;


import java.util.ArrayList;
import java.util.List;

/**
 * Сначала берем лок на CCL, затем берем лок на нума ноде, затем берем глобальный
 */
public class HMCS_CCL_PLUS_NUMA_HIERARCHY extends AbstractHmcs {
    
    public HMCS_CCL_PLUS_NUMA_HIERARCHY(boolean overSubscription, boolean isLight) {
        super(overSubscription, isLight, Utils::kungpengGetClusterID);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int cclSize = 4;
        int numaNodesCount;
        if (availableProcessors == 128 || availableProcessors == 96) {
            numaNodesCount = 4;
        } else {
            numaNodesCount = 2;
        }
        int cclPerNuma = (availableProcessors / cclSize) / numaNodesCount;
        var root = new HNode(null);
        List<HNode> numaNodes = new ArrayList<>();
        for (int i = 0; i < numaNodesCount; i++) {
            numaNodes.add(new HNode(root));
        }
        for (int i = 0; i < availableProcessors / cclSize; i++) {
            leafs.add(new HNode(numaNodes.get(i / cclPerNuma)));
        }
    }
}
