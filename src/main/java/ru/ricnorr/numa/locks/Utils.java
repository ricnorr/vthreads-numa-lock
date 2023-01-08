package ru.ricnorr.numa.locks;

import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;

public class Utils {

    public static int WAIT_THRESHOLD = 4096;

    private final static int GET_CPU_ARM_SYSCALL = 168;

    private final static int GET_CPU_x86_SYSCALL = 309;

    static int getClusterID() {
        int res;
        final IntByReference numaNode = new IntByReference();

        if (Platform.isARM()) {
            res = CLibrary.INSTANCE.syscall(GET_CPU_ARM_SYSCALL, null, numaNode, null);
        } else {
            res = CLibrary.INSTANCE.syscall(GET_CPU_x86_SYSCALL, null, numaNode,null);
        }
        if (res < 0) {
            throw new IllegalStateException("Cannot make syscall getcpu");
        }
        return numaNode.getValue();
    }

    public static int spinWait(int spinCounter) {
        for (int i = 0; i < spinCounter; i++) {
            Thread.onSpinWait();
        }
        if (spinCounter > WAIT_THRESHOLD) {
            Thread.yield();
            return 1;
        }
        spinCounter *= 2;
        return spinCounter;
    }
}
