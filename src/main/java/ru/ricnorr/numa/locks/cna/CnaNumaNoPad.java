package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.Utils;


public class CnaNumaNoPad extends AbstractCna<CNANodeNoPad> {

    public CnaNumaNoPad() {
        super(Utils::getNumaNodeId, CNANodeNoPad::new);
    }
}
