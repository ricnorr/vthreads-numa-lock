package ru.ricnorr.numa.locks.cna;

import ru.ricnorr.numa.locks.Utils;

public class CNACclWithPad extends AbstractCNA<CNANodeWithPad> {
    public CNACclWithPad() {
        super(Utils::getKunpengCCLId, CNANodeWithPad::new);
    }
}
