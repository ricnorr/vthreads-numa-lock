package ru.ricnorr.numa.locks.hclh;

import ru.ricnorr.numa.locks.Utils;

public class HclhCclWithPad extends AbstractHCLHLock<HCLHNodeWithPad> {
    public HclhCclWithPad() {
        super(HCLHNodeWithPad::new, Utils::getKunpengCCLId);
    }
}

