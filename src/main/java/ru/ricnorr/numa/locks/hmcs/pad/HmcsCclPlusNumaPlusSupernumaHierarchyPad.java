package ru.ricnorr.numa.locks.hmcs.pad;

import ru.ricnorr.numa.locks.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Взять лок на CCL, затем на нума ноде, затем на супер-нума ноде, затем глобальный
 * На 48 корной машинке нет смысла считать, только на 96 и 128
 */
public class HmcsCclPlusNumaPlusSupernumaHierarchyPad extends AbstractHmcsPad {

    public HmcsCclPlusNumaPlusSupernumaHierarchyPad(boolean overSubscription, boolean isLight) {
        super(overSubscription, isLight, Utils::getKunpengCCLId, Runtime.getRuntime().availableProcessors() / CCL_SIZE);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int numaNodesCount = 4;
        int cclPerNuma = (availableProcessors / CCL_SIZE) / numaNodesCount;
        var root = new HNode(null);
        List<HNode> superNumaNodes = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            superNumaNodes.add(new HNode(root));
        }
        List<HNode> numaNodes = new ArrayList<>();
        for (int i = 0; i < numaNodesCount; i++) {
            numaNodes.add(new HNode(superNumaNodes.get(i / 2)));
        }
        for (int i = 0; i < availableProcessors / CCL_SIZE; i++) {
            leafs[i] = new HNode(numaNodes.get(i / cclPerNuma));
        }
    }
}
