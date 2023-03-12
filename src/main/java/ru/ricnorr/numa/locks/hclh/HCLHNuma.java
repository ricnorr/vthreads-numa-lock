package ru.ricnorr.numa.locks.hclh;

import ru.ricnorr.numa.locks.Utils;

public class HCLHNuma extends AbstractHCLHLock<HCLHNodeWithPad> {
    public HCLHNuma() {
        super(HCLHNodeWithPad::new, Utils::getNumaNodeId);
    }
}

