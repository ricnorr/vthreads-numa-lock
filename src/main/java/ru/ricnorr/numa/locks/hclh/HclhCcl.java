package ru.ricnorr.numa.locks.hclh;

import ru.ricnorr.numa.locks.Utils;

public class HclhCcl extends AbstractHCLHLock {
    public HclhCcl(boolean isLight) {
        super(isLight, Utils::kungpengGetClusterID);
    }
}
