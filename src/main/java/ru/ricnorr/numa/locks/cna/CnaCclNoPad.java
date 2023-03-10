package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.Utils;

public class CnaCclNoPad extends AbstractCna<CNANodeNoPad> {
    public CnaCclNoPad() {
        super(Utils::getKunpengCCLId, CNANodeNoPad::new);
    }
}
