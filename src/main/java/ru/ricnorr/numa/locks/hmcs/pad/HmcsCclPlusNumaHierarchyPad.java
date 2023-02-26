package ru.ricnorr.numa.locks.hmcs.pad;


import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Сначала берем лок на CCL, затем берем лок на нума ноде, затем берем глобальный
 */
public class HmcsCclPlusNumaHierarchyPad extends AbstractHmcsPad {

    public HmcsCclPlusNumaHierarchyPad(boolean overSubscription, boolean isLight) {
        super(overSubscription, isLight, Utils::kungpengGetClusterID, Runtime.getRuntime().availableProcessors() / CCL_SIZE);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int numaNodesCount;
        if (availableProcessors == 128 || availableProcessors == 96) {
            numaNodesCount = 4;
        } else {
            numaNodesCount = 2;
        }
        int cclPerNuma = (availableProcessors / CCL_SIZE) / numaNodesCount;
        var root = new HNode(null);
        List<HNode> numaNodes = new ArrayList<>();
        for (int i = 0; i < numaNodesCount; i++) {
            numaNodes.add(new HNode(root));
        }
        for (int i = 0; i < availableProcessors / CCL_SIZE; i++) {
            leafs[i] = new HNode(numaNodes.get(i / cclPerNuma));
        }
    }
}
