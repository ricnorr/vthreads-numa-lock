package ru.ricnorr.numa.locks.hclh;

import ru.ricnorr.numa.locks.Utils;

public class HCLHCcl extends AbstractHCLHLock<HCLHNodeWithPad> {
    public HCLHCcl() {
        super(HCLHNodeWithPad::new, Utils::getKunpengCCLId);
    }
}

