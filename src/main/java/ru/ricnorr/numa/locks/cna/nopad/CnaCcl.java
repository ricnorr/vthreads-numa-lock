package ru.ricnorr.numa.locks.cna.nopad;

import ru.ricnorr.numa.locks.Utils;

public class CnaCcl extends AbstractCna {
    public CnaCcl(boolean useLight) {
        super(useLight, Utils::kungpengGetClusterID);
    }
}
