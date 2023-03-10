package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.Utils;


public class CNANumaWithPad extends AbstractCNA<CNANodeWithPad> {

    public CNANumaWithPad() {
        super(Utils::getNumaNodeId, CNANodeWithPad::new);
    }
}
