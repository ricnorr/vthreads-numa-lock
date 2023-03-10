package ru.ricnorr.numa.locks.hclh.pad;

import ru.ricnorr.numa.locks.Utils;

public class HclhCclPad extends AbstractHCLHLockPad {
    public HclhCclPad(boolean isLight) {
        super(isLight, Utils::getKunpengCCLId);
    }
}
