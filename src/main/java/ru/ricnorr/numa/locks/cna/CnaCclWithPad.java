package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.Utils;

public class CnaCclWithPad extends AbstractCna<CNANodeWithPad> {
    public CnaCclWithPad() {
        super(Utils::getKunpengCCLId, CNANodeWithPad::new);
    }
}
