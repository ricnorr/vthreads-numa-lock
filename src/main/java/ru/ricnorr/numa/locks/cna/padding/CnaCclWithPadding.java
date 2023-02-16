package ru.ricnorr.numa.locks.cna.padding;

import ru.ricnorr.numa.locks.Utils;

public class CnaCclWithPadding extends AbstractCnaWithPadding {
    public CnaCclWithPadding(boolean useLight) {
        super(useLight, Utils::kungpengGetClusterID);
    }
}
