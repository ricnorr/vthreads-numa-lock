package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.Utils;


public class CnaNumaWithPad extends AbstractCna<CNANodeWithPad> {

    public CnaNumaWithPad() {
        super(Utils::getNumaNodeId, CNANodeWithPad::new);
    }
}
