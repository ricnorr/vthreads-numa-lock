package ru.ricnorr.numa.locks.cna.pad;

import ru.ricnorr.numa.locks.Utils;


public class CnaNumaWithContendedPadding extends AbstractCnaWithContendedPadding {

    public CnaNumaWithContendedPadding(boolean isLight) {
        super(isLight, Utils::getClusterID);
    }
}
