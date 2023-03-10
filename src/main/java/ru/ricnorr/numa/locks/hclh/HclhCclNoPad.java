package ru.ricnorr.numa.locks.hclh;

import ru.ricnorr.numa.locks.Utils;

public class HclhCclNoPad extends AbstractHCLHLock<HCLHNodeNoPad> {
    public HclhCclNoPad() {
        super(HCLHNodeNoPad::new, Utils::getKunpengCCLId);
    }
}
