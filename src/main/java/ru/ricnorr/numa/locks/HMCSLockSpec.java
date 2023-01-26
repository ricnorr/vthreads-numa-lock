package ru.ricnorr.numa.locks;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import ru.ricnorr.benchmarks.LockType;

public class HMCSLockSpec {
    public long ccl = 4;
    public static String CCL = "ccl";

    public HMCSLockSpec(String params) {
        var obj = (JSONObject) JSONValue.parse(params);
        if (obj.get(CCL) != null) {
            this.ccl = (Long) obj.get(CCL);
        }
    }

    @Override
    public String toString() {
        return LockType.HMCS + "_" + "ccl=" + ccl;
    }

}
