package ru.ricnorr.numa.locks;

import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Utils {

    public static final MethodHandle GET_CARRIER_THREAD_METHOD_HANDLE;
    public static final MethodHandle GET_BY_THREAD_FROM_THREAD_LOCAL_METHOD_HANDLE;

    public static final MethodHandle SET_BY_THREAD_TO_THREAD_LOCAL_METHOD_HANDLE;
    private final static int GET_CPU_ARM_SYSCALL = 168;
    private final static int GET_CPU_x86_SYSCALL = 309;
    public static int WAIT_THRESHOLD = 4096;

    static {
        Method currentCarrierThreadMethod;

        try {
            currentCarrierThreadMethod = Thread.class.getDeclaredMethod("currentCarrierThread");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        currentCarrierThreadMethod.setAccessible(true);
        try {
            GET_CARRIER_THREAD_METHOD_HANDLE = lookup.unreflect(currentCarrierThreadMethod);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Method getByThreadFromThreadLocalMethod;
        try {
            getByThreadFromThreadLocalMethod = ThreadLocal.class.getDeclaredMethod("get", Thread.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        getByThreadFromThreadLocalMethod.setAccessible(true);
        try {
            GET_BY_THREAD_FROM_THREAD_LOCAL_METHOD_HANDLE = lookup.unreflect(getByThreadFromThreadLocalMethod);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }


        Method setByThreadToThreadLocalMethod;
        try {
            setByThreadToThreadLocalMethod = ThreadLocal.class.getDeclaredMethod("set", Thread.class, Object.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        setByThreadToThreadLocalMethod.setAccessible(true);
        try {
            SET_BY_THREAD_TO_THREAD_LOCAL_METHOD_HANDLE = lookup.unreflect(setByThreadToThreadLocalMethod);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

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

    public static int kungpengGetClusterID() {
        int cpuId = getCpuID();
        return cpuId / 4;
    }

    public static int spinWaitYield(int spinCounter) {
        for (int i = 0; i < spinCounter; i++) {
        }
        if (spinCounter > WAIT_THRESHOLD) {
            Thread.yield();
            return 1;
        }
        spinCounter *= 2;
        return spinCounter;
    }


    public static double median(Collection<Double> numbers) {
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute median on empty collection of numbers");
        }
        List<Double> numbersList = new ArrayList<>(numbers);
        Collections.sort(numbersList);
        int middle = numbersList.size() / 2;
        if (numbersList.size() % 2 == 0) {
            return 0.5 * (numbersList.get(middle) + numbersList.get(middle - 1));
        } else {
            return numbersList.get(middle);
        }
    }

    public static Thread getCurrentCarrierThread() {

        try {
            return (Thread) GET_CARRIER_THREAD_METHOD_HANDLE.invoke();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getByThreadFromThreadLocal(ThreadLocal<T> tl, Thread thread) {
        try {
            return (T) GET_BY_THREAD_FROM_THREAD_LOCAL_METHOD_HANDLE.invoke(tl, thread);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void setByThreadToThreadLocal(ThreadLocal<T> tl, Thread thread, Object obj) {
        try {
            SET_BY_THREAD_TO_THREAD_LOCAL_METHOD_HANDLE.invoke(tl, thread, obj);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
