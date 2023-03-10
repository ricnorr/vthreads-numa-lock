package ru.ricnorr.numa.locks.hclh;

import ru.ricnorr.numa.locks.Utils;

public class HCLHCclWithPad extends AbstractHCLHLock<HCLHNodeWithPad> {
    public HCLHCclWithPad() {
        super(HCLHNodeWithPad::new, Utils::getKunpengCCLId);
    }
}

