package ru.ricnorr.numa.locks.hclh;


import ru.ricnorr.numa.locks.Utils;

public class HCLHNumaNoPad extends AbstractHCLHLock<HCLHNodeNoPad> {
    public HCLHNumaNoPad() {
        super(HCLHNodeNoPad::new, Utils::getNumaNodeId);
    }
}

