package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.Utils;

public class CNACcl extends AbstractCNA<CNANode> {
    public CNACcl() {
        super(Utils::getKunpengCCLId, CNANode::new);
    }
}
