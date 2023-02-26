package ru.ricnorr.numa.locks.cna.pad;

import ru.ricnorr.numa.locks.Utils;

public class CnaCclWithContendedPadding extends AbstractCnaWithContendedPadding {
    public CnaCclWithContendedPadding(boolean useLight) {
        super(useLight, Utils::kungpengGetClusterID);
    }
}
