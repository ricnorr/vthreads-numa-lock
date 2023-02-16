package ru.ricnorr.numa.locks.hmcs;

import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Взять лок на CCL, затем на нума ноде, затем на супер-нума ноде, затем глобальный
 * На 48 корной машинке нет смысла считать, только на 96 и 128
 */
public class HmcsCclPlusNumaPlusSupernumaHierarchy extends AbstractHmcs {

    public HmcsCclPlusNumaPlusSupernumaHierarchy(boolean overSubscription, boolean isLight) {
        super(overSubscription, isLight, Utils::kungpengGetClusterID);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int cclSize = 4;
        int numaNodesCount = 4;
        int cclPerNuma = (availableProcessors / cclSize) / numaNodesCount;
        var root = new HNode(null);
        List<HNode> superNumaNodes = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            superNumaNodes.add(new HNode(root));
        }
        List<HNode> numaNodes = new ArrayList<>();
        for (int i = 0; i < numaNodesCount; i++) {
            numaNodes.add(new HNode(superNumaNodes.get(i / 2)));
        }
        for (int i = 0; i < availableProcessors / cclSize; i++) {
            leafs.add(new HNode(numaNodes.get(i / cclPerNuma)));
        }
    }
}
