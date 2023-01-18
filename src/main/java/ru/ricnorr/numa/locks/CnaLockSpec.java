package ru.ricnorr.numa.locks;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import ru.ricnorr.benchmarks.LockType;

public class CnaLockSpec {
    public boolean useJavaAtomics = true;
    public static String USE_JAVA_ATOMICS = "javatom";

    public boolean useParkingOnSpin = true;
    public static String USE_PARKING_ON_SPIN = "park";

    public long spinThreshold = 512;

    public static String SPIN_THRESHOLD = "thres";

    public boolean kunpengNuma = true;

    public static String KUNPENG_NUMA_DIST = "numa";

    public CnaLockSpec(String params) {
        var obj = (JSONObject) JSONValue.parse(params);
        if (obj.get(USE_JAVA_ATOMICS) != null) {
            this.useJavaAtomics = (Boolean) obj.get(USE_JAVA_ATOMICS);
        }
        if (obj.get(USE_PARKING_ON_SPIN) != null) {
            this.useParkingOnSpin = (Boolean) obj.get(USE_PARKING_ON_SPIN);
        }
        if (obj.get(SPIN_THRESHOLD) != null) {
            this.spinThreshold = (Long) obj.get(SPIN_THRESHOLD);
        }
        if (obj.get(KUNPENG_NUMA_DIST) != null) {
            this.kunpengNuma = (Boolean) obj.get(KUNPENG_NUMA_DIST);
        }
    }

    @Override
    public String toString() {
        return LockType.CNA + "_" + "useJavaAtomics=" + useJavaAtomics;
    }
}
