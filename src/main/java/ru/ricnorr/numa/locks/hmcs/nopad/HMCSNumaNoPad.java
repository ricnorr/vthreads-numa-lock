package ru.ricnorr.numa.locks.hmcs.nopad;


import ru.ricnorr.numa.locks.Utils;
import ru.ricnorr.numa.locks.hmcs.AbstractHMCS;

public class HMCSNumaNoPad extends AbstractHMCS<HMCSQNodeNoPad> {


    public HMCSNumaNoPad() {
        super(HMCSQNodeNoPad::new, Utils::getNumaNodeId, Utils.NUMA_NODES_CNT);
        var root = new HNode(null, new HMCSQNodeNoPad());
        for (int i = 0; i < Utils.NUMA_NODES_CNT; i++) {
            leafs[i] = new HNode(root, new HMCSQNodeNoPad());
        }
    }
}
