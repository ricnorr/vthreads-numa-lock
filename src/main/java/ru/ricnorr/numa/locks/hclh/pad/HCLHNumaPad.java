package ru.ricnorr.numa.locks.hclh.pad;


import ru.ricnorr.numa.locks.Utils;

public class HCLHNumaPad extends AbstractHCLHLockPad {
    public HCLHNumaPad(boolean isLight) {
        super(isLight, Utils::getClusterID);
    }
}

