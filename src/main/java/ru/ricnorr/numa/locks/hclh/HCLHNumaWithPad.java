package ru.ricnorr.numa.locks.hclh;

import ru.ricnorr.numa.locks.Utils;

public class HCLHNumaWithPad extends AbstractHCLHLock<HCLHNodeWithPad> {
    public HCLHNumaWithPad() {
        super(HCLHNodeWithPad::new, Utils::getNumaNodeId);
    }
}

