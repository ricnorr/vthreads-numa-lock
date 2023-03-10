package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.Utils;

public class CNACclNoPad extends AbstractCNA<CNANodeNoPad> {
    public CNACclNoPad() {
        super(Utils::getKunpengCCLId, CNANodeNoPad::new);
    }
}
