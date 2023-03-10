package ru.ricnorr.numa.locks.hclh;

import ru.ricnorr.numa.locks.Utils;

public class HCLHCclNoPad extends AbstractHCLHLock<HCLHNodeNoPad> {
    public HCLHCclNoPad() {
        super(HCLHNodeNoPad::new, Utils::getKunpengCCLId);
    }
}
