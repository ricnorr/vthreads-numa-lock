package ru.ricnorr.numa.locks;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import ru.ricnorr.benchmarks.LockType;

public class CnaLockSpec {
    public boolean useJavaAtomics = true;
    public static String USE_JAVA_ATOMICS = "useJavaAtomics";

    public boolean useParkingOnSpin = true;
    public static String USE_PARKING_ON_SPIN = "useParkingOnSpin";

    public int spinThreshold = 512;

    public static String SPIN_THRESHOLD = "spinThreshold";

    public CnaLockSpec(String params) {
        var obj = (JSONObject) JSONValue.parse(params);
        this.useJavaAtomics = (Boolean) obj.get(USE_JAVA_ATOMICS);
        this.useParkingOnSpin = (Boolean) obj.get(USE_PARKING_ON_SPIN);
        this.spinThreshold = (Integer) obj.get(SPIN_THRESHOLD);
    }

    @Override
    public String toString() {
        return LockType.CNA + "_" + "useJavaAtomics=" + useJavaAtomics;
    }
}
