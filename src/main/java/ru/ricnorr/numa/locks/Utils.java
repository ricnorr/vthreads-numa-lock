package ru.ricnorr.numa.locks;

import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;

import java.util.concurrent.locks.LockSupport;

public class Utils {

    public static int WAIT_THRESHOLD = 4096;

    private final static int GET_CPU_ARM_SYSCALL = 168;

    private final static int GET_CPU_x86_SYSCALL = 309;

    public static int getClusterID() {
        int res;
        final IntByReference numaNode = new IntByReference();
        final IntByReference cpu = new IntByReference();
        if (Platform.isARM()) {
            res = CLibrary.INSTANCE.syscall(GET_CPU_ARM_SYSCALL, cpu, numaNode, null);
        } else {
            res = CLibrary.INSTANCE.syscall(GET_CPU_x86_SYSCALL, cpu, numaNode, null);
        }
        if (res < 0) {
            throw new IllegalStateException("Cannot make syscall getcpu");
        }
        return numaNode.getValue();
    }

    public static int getCpuID() {
        int res;
        final IntByReference numaNode = new IntByReference();
        final IntByReference cpu = new IntByReference();
        if (Platform.isARM()) {
            res = CLibrary.INSTANCE.syscall(GET_CPU_ARM_SYSCALL, cpu, numaNode, null);
        } else {
            res = CLibrary.INSTANCE.syscall(GET_CPU_x86_SYSCALL, cpu, numaNode, null);
        }
        if (res < 0) {
            throw new IllegalStateException("Cannot make syscall getcpu");
        }
        return cpu.getValue();
    }

    public static int spinWaitYield(int spinCounter) {
        for (int i = 0; i < spinCounter; i++) {
            //Thread.onSpinWait();
        }
        if (spinCounter > WAIT_THRESHOLD) {
            Thread.yield();
            return 1;
        }
        spinCounter *= 2;
        return spinCounter;
    }

    public static int spinWaitPark(int spinCounter) {
        if (spinCounter < 512) {
            for (int i = 0; i < spinCounter; i++) {
                Thread.onSpinWait();
            }
            spinCounter *= 2;
            return spinCounter;
        } else {
            LockSupport.park();
            return 1024;
        }
    }
}
