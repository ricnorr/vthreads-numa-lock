package ru.ricnorr.numa.locks;

import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import ru.ricnorr.benchmarks.BenchmarkException;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.basic.*;
import ru.ricnorr.numa.locks.cna.CNACclNoPad;
import ru.ricnorr.numa.locks.cna.CNACclWithPad;
import ru.ricnorr.numa.locks.cna.CNANumaNoPad;
import ru.ricnorr.numa.locks.cna.CNANumaWithPad;
import ru.ricnorr.numa.locks.combination.CombinationLock;
import ru.ricnorr.numa.locks.hclh.HCLHCclNoPad;
import ru.ricnorr.numa.locks.hclh.HCLHCclWithPad;
import ru.ricnorr.numa.locks.hclh.HCLHNumaNoPad;
import ru.ricnorr.numa.locks.hclh.HCLHNumaWithPad;
import ru.ricnorr.numa.locks.hmcs.*;
import ru.ricnorr.numa.locks.reentrant.NumaReentrantLock;
import ru.ricnorr.numa.locks.tas_cna.TtasCclAndCnaNuma;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    public static final MethodHandle GET_CARRIER_THREAD_METHOD_HANDLE;
    public static final MethodHandle GET_BY_THREAD_FROM_THREAD_LOCAL_METHOD_HANDLE;

    public static final MethodHandle SET_BY_THREAD_TO_THREAD_LOCAL_METHOD_HANDLE;
    private final static int GET_CPU_ARM_SYSCALL = 168;
    private final static int GET_CPU_x86_SYSCALL = 309;

    public static int CCL_SIZE = 4;

    public static int NUMA_NODES_CNT = getNumaNodesCnt();

    public static int CCL_CNT = Runtime.getRuntime().availableProcessors() / CCL_SIZE;

    static {
        GET_CARRIER_THREAD_METHOD_HANDLE = getMethodHandle(Thread.class, "currentCarrierThread");
        GET_BY_THREAD_FROM_THREAD_LOCAL_METHOD_HANDLE = getMethodHandle(ThreadLocal.class, "get", Thread.class);
        SET_BY_THREAD_TO_THREAD_LOCAL_METHOD_HANDLE = getMethodHandle(ThreadLocal.class, "set", Thread.class, Object.class);
    }

    private static MethodHandle getMethodHandle(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Method method;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        method.setAccessible(true);
        try {
            return lookup.unreflect(method);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getNumaNodeId() {
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

    public static int getCpuId() {
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

    public static int getKunpengCCLId() {
        int cpuId = getCpuId();
        return cpuId / CCL_SIZE;
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

    public static int getNumaNodesCnt() {
        return new SystemInfo().getHardware().getProcessor().getLogicalProcessors()
                .stream().map(CentralProcessor.LogicalProcessor::getNumaNode)
                .collect(Collectors.toSet()).size();
    }

    public static NumaLock initLock(LockType lockType) {
        switch (lockType) {
            case UNFAIR_REENTRANT -> {
                return new NumaReentrantLock(false);
            }
            case FAIR_REENTRANT -> {
                return new NumaReentrantLock(true);
            }
            /**
             * MCS
             */
            case MCS -> {
                return new MCS();
            }
            case TAS -> {
                return new TestAndSetLock();
            }
            case TTAS -> {
                return new TestTestAndSetLock();
            }
            case TICKET -> {
                return new TicketLock();
            }
            case CLH -> {
                return new CLHLock();
            }
            /**
             * CNA
             */
            case CNA_NUMA -> {
                return new CNANumaNoPad();
            }
            case CNA_CCL -> {
                return new CNACclNoPad();
            }
            case CNA_CCL_PAD -> {
                return new CNACclWithPad();
            }
            case CNA_NUMA_PAD -> {
                return new CNANumaWithPad();
            }
            /**
             * HCLH
             */
            case HCLH_CCL -> {
                return new HCLHCclNoPad();
            }
            case HCLH_NUMA -> {
                return new HCLHNumaNoPad();
            }
            case HCLH_CCL_PAD -> {
                return new HCLHCclWithPad();
            }
            case HCLH_NUMA_PAD -> {
                return new HCLHNumaWithPad();
            }
            /**
             * HMCS
             */
            case HMCS_CCL_NUMA -> {
                return new HMCSCclNumaNoPad();
            }
            case HMCS_CCL_NUMA_PAD -> {
                return new HMCSCclNumaWithPad();
            }
            case HMCS_CCL_NUMA_SUPERNUMA -> {
                return new HMCSCclNumaSupernumaNoPad();
            }
            case HMCS_CCL_NUMA_SUPERNUMA_PAD -> {
                return new HMCSCclNumaSupernumaWithPad();
            }
            case HMCS_CCL -> {
                return new HMCSCclNoPad();
            }
            case HMCS_CCL_PAD -> {
                return new HMCSCclWithPad();
            }
            case HMCS_NUMA -> {
                return new HMCSNumaNoPad();
            }
            case HMCS_NUMA_PAD -> {
                return new HMCSNumaWithPad();
            }
            case TTAS_CCL_PLUS_CNA_NUMA -> {
                return new TtasCclAndCnaNuma();
            }
            case COMB_CNA_CCL_MCS_NUMA -> {
                return new CombinationLock(
                        List.of(
                                new CombinationLock.CombinationLockLevelDescription(
                                        LockType.MCS,
                                        Utils.NUMA_NODES_CNT
                                ),
                                new CombinationLock.CombinationLockLevelDescription(
                                        LockType.CNA_CCL,
                                        0
                                )
                        ),
                        Utils::getNumaNodeId
                );
            }
            case COMB_HCLH_CCL_MCS_NUMA -> {
                return new CombinationLock(
                        List.of(
                                new CombinationLock.CombinationLockLevelDescription(
                                        LockType.MCS,
                                        Utils.NUMA_NODES_CNT
                                ),
                                new CombinationLock.CombinationLockLevelDescription(
                                        LockType.HCLH_CCL,
                                        0
                                )

                        ),
                        Utils::getNumaNodeId
                );
            }
            default -> throw new BenchmarkException("Can't init lockType " + lockType.name());
        }
    }
}
