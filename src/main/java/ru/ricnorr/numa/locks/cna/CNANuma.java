package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.Utils;


public class CNANuma extends AbstractCNA<CNANode> {

    public CNANuma() {
        super(Utils::getNumaNodeId, CNANode::new);
    }
}
