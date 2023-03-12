package ru.ricnorr.numa.locks.cna.pad;

import ru.ricnorr.numa.locks.Utils;
import ru.ricnorr.numa.locks.cna.AbstractCNA;

public class CNACclWithPad extends AbstractCNA<CNANodeWithPad> {
    public CNACclWithPad() {
        super(Utils::getKunpengCCLId, CNANodeWithPad::new);
    }
}
