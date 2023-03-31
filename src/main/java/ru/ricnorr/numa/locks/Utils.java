package ru.ricnorr.numa.locks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import ru.ricnorr.benchmarks.BenchmarkException;
import ru.ricnorr.benchmarks.LockType;
import ru.ricnorr.numa.locks.basic.CLH;
import ru.ricnorr.numa.locks.basic.MCS;
import ru.ricnorr.numa.locks.basic.MCS_PARK;
import ru.ricnorr.numa.locks.basic.MCS_SLEEP;
import ru.ricnorr.numa.locks.basic.TAS;
import ru.ricnorr.numa.locks.basic.TTAS;
import ru.ricnorr.numa.locks.basic.Ticket;
import ru.ricnorr.numa.locks.cna.CNACcl;
import ru.ricnorr.numa.locks.cna.CNANuma;
import ru.ricnorr.numa.locks.cna.pad.CNACclWithPad;
import ru.ricnorr.numa.locks.cna.pad.CNANumaWithPad;
import ru.ricnorr.numa.locks.cna_sleep.CNANumaSleep;
import ru.ricnorr.numa.locks.combination.CombinationLock;
import ru.ricnorr.numa.locks.hclh.HCLHCcl;
import ru.ricnorr.numa.locks.hclh.HCLHCclNoPad;
import ru.ricnorr.numa.locks.hclh.HCLHNuma;
import ru.ricnorr.numa.locks.hclh.HCLHNumaNoPad;
import ru.ricnorr.numa.locks.hmcs.HMCSCcl;
import ru.ricnorr.numa.locks.hmcs.HMCSCclNuma;
import ru.ricnorr.numa.locks.hmcs.HMCSCclNumaSupernuma;
import ru.ricnorr.numa.locks.hmcs.HMCSNuma;
import ru.ricnorr.numa.locks.hmcs.HMCSNumaSupernuma;
import ru.ricnorr.numa.locks.hmcs.nopad.HMCSCclNoPad;
import ru.ricnorr.numa.locks.hmcs.nopad.HMCSCclNumaNoPad;
import ru.ricnorr.numa.locks.hmcs.nopad.HMCSCclNumaSupernumaNoPad;
import ru.ricnorr.numa.locks.hmcs.nopad.HMCSNumaNoPad;
import ru.ricnorr.numa.locks.hmcs_comb.HMCSComb;
import ru.ricnorr.numa.locks.hmcs_sleep.HMCSCclNumaSleep;
import ru.ricnorr.numa.locks.hmcs_v.HMCSNumaV;
import ru.ricnorr.numa.locks.reentrant.NumaReentrantLock;

public class Utils {

  public static final MethodHandle GET_CARRIER_THREAD_METHOD_HANDLE;
  public static final MethodHandle GET_BY_THREAD_FROM_THREAD_LOCAL_METHOD_HANDLE;

  public static final MethodHandle SET_BY_THREAD_TO_THREAD_LOCAL_METHOD_HANDLE;
  private final static int GET_CPU_ARM_SYSCALL = 168;
  private final static int GET_CPU_x86_SYSCALL = 309;

  public static int CCL_SIZE = 4;

  public static int NUMA_NODES_CNT = getNumaNodesCnt();

  public static int CCL_CNT = Runtime.getRuntime().availableProcessors() / CCL_SIZE;

  public static int CORES_CNT = Runtime.getRuntime().availableProcessors();

  public static int CORES_PER_NUMA = CORES_CNT / NUMA_NODES_CNT;

  public static int CCL_PER_NUMA = CCL_CNT / NUMA_NODES_CNT;


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
    return initLock(lockType, 0);
  }

  public static NumaLock initLock(LockType lockType, int threads) {
    switch (lockType) {
      // Standard locks
      case UNFAIR_REENTRANT -> {
        return new NumaReentrantLock(false);
      }
      case FAIR_REENTRANT -> {
        return new NumaReentrantLock(true);
      }
      case MCS -> {
        return new MCS();
      }
      case MCS_SLEEP -> {
        return new MCS_SLEEP();
      }
      case MCS_PARK -> {
        return new MCS_PARK();
      }
      case TAS -> {
        return new TAS();
      }
      case TTAS -> {
        return new TTAS();
      }
      case TICKET -> {
        return new Ticket();
      }
      case CLH -> {
        return new CLH();
      }
      // CNA
      case CNA_NUMA -> {
        return new CNANuma();
      }
      case CNA_CCL -> {
        return new CNACcl();
      }
      case CNA_NUMA_SLEEP -> {
        return new CNANumaSleep(threads > Runtime.getRuntime().availableProcessors(), true);
      }
      case CNA_NUMA_SLEEP_2 -> {
        return new CNANumaSleep(threads > Runtime.getRuntime().availableProcessors(), false);
      }
      // CNA PAD
      case CNA_CCL_PAD -> {
        return new CNACclWithPad();
      }
      case CNA_NUMA_PAD -> {
        return new CNANumaWithPad();
      }
      // HCLH
      case HCLH_CCL -> {
        return new HCLHCcl();
      }
      case HCLH_NUMA -> {
        return new HCLHNuma();
      }
      // HCLH NO PAD
      case HCLH_CCL_NOPAD -> {
        return new HCLHCclNoPad();
      }
      case HCLH_NUMA_NOPAD -> {
        return new HCLHNumaNoPad();
      }
      // HMCS
      case HMCS_CCL_NUMA -> {
        return new HMCSCclNuma();
      }
      case HMCS_CCL_NUMA_SUPERNUMA -> {
        return new HMCSCclNumaSupernuma();
      }
      case HMCS_CCL -> {
        return new HMCSCcl();
      }
      case HMCS_NUMA -> {
        return new HMCSNuma();
      }
      case HMCS_NUMA_SUPERNUMA -> {
        return new HMCSNumaSupernuma();
      }
      case HMCS_CCL_NUMA_V2 -> {
        return new HMCSCclNumaSleep(threads > Runtime.getRuntime().availableProcessors());
      }
      case HMCS_NUMA_V3 -> {
        return new HMCSNumaV(threads > Runtime.getRuntime().availableProcessors(), true, Utils.CORES_PER_NUMA / 2);
      }
      case HMCS_NUMA_V4 -> {
        return new HMCSNumaV(threads > Runtime.getRuntime().availableProcessors(), false, Utils.CORES_PER_NUMA / 2);
      }
      case HMCS_NUMA_V5 -> {
        return new HMCSNumaV(threads > Runtime.getRuntime().availableProcessors(), true, Utils.CORES_PER_NUMA / 4);
      }
      case HMCS_NUMA_V6 -> {
        return new HMCSNumaV(threads > Runtime.getRuntime().availableProcessors(), true, Utils.CORES_PER_NUMA / 8);
      }
      case HMCS_CUSTOM -> {
        return new HMCSComb();
      }
      // HMCS NO PAD
      case HMCS_CCL_NUMA_UNPAD -> {
        return new HMCSCclNumaNoPad();
      }
      case HMCS_CCL_NUMA_SUPERNUMA_UNPAD -> {
        return new HMCSCclNumaSupernumaNoPad();
      }
      case HMCS_CCL_UNPAD -> {
        return new HMCSCclNoPad();
      }
      case HMCS_NUMA_UNPAD -> {
        return new HMCSNumaNoPad();
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
      case COMB_TTAS_CCL_HCLH_NUMA -> {
        return new CombinationLock(
            List.of(
                new CombinationLock.CombinationLockLevelDescription(
                    LockType.HCLH_NUMA,
                    Utils.CCL_CNT
                ),
                new CombinationLock.CombinationLockLevelDescription(
                    LockType.TTAS,
                    0
                )
            ),
            Utils::getKunpengCCLId
        );
      }
      case COMB_TTAS_CCL_CNA_NUMA -> {
        return new CombinationLock(
            List.of(
                new CombinationLock.CombinationLockLevelDescription(
                    LockType.CNA_NUMA,
                    Utils.CCL_CNT
                ),
                new CombinationLock.CombinationLockLevelDescription(
                    LockType.TTAS,
                    0
                )
            ),
            Utils::getKunpengCCLId
        );
      }
      case COMB_TTAS_NUMA_MCS -> {
        return new CombinationLock(
            List.of(
                new CombinationLock.CombinationLockLevelDescription(
                    LockType.MCS,
                    Utils.NUMA_NODES_CNT
                ),
                new CombinationLock.CombinationLockLevelDescription(
                    LockType.TTAS,
                    0
                )
            ),
            Utils::getNumaNodeId
        );
      }
      case TTAS_CCL_MCS -> {
        return new TTAS_CCL_MCS();
      }

      default -> throw new BenchmarkException("Can't init lockType " + lockType.name());
    }
  }
}
