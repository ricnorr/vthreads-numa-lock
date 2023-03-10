package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.Utils;


public class CNANumaNoPad extends AbstractCNA<CNANodeNoPad> {

    public CNANumaNoPad() {
        super(Utils::getNumaNodeId, CNANodeNoPad::new);
    }
}
