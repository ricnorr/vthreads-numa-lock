package ru.ricnorr.numa.locks.cna.pad;

import ru.ricnorr.numa.locks.Utils;
import ru.ricnorr.numa.locks.cna.AbstractCNA;


public class CNANumaWithPad extends AbstractCNA<CNANodeWithPad> {

    public CNANumaWithPad() {
        super(Utils::getNumaNodeId, CNANodeWithPad::new);
    }
}
