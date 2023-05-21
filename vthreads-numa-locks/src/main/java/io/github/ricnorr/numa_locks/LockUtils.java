package io.github.ricnorr.numa_locks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.stream.Collectors;

import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

/**
 * Utilities for implementing locks
 */
public class LockUtils {

  private static final MethodHandle GET_CARRIER_THREAD_METHOD_HANDLE;
  private static final MethodHandle GET_BY_THREAD_FROM_THREAD_LOCAL_METHOD_HANDLE;

  private static final MethodHandle SET_BY_THREAD_TO_THREAD_LOCAL_METHOD_HANDLE;
  private final static int GET_CPU_ARM_SYSCALL = 168;
  private final static int GET_CPU_x86_SYSCALL = 309;

  private static final int CCL_SIZE = 4;
  /**
   * The number of NUMA nodes available in the system
   */
  public static int NUMA_NODES_CNT = getNumaNodesCnt();

  /**
   * <p>The number of CCL groups in the system (for Kunpeng-920)
   *
   * @see <a href="https://jianbinfang.github.io/files/2021-09-02-jcst.pdf">CCL</a>
   */
  public static int CCL_CNT = Runtime.getRuntime().availableProcessors() / CCL_SIZE;
  private static int CCL_PER_NUMA = CCL_CNT / NUMA_NODES_CNT;

  static {
    GET_CARRIER_THREAD_METHOD_HANDLE = getMethodHandle(Thread.class, "currentCarrierThread");
    GET_BY_THREAD_FROM_THREAD_LOCAL_METHOD_HANDLE = getMethodHandle(ThreadLocal.class, "get", Thread.class);
    SET_BY_THREAD_TO_THREAD_LOCAL_METHOD_HANDLE = getMethodHandle(ThreadLocal.class, "set", Thread.class, Object.class);
  }

  private static int getNumaNodesCnt() {
    return new SystemInfo().getHardware().getProcessor().getLogicalProcessors()
        .stream().map(CentralProcessor.LogicalProcessor::getNumaNode)
        .collect(Collectors.toSet()).size();
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

  /**
   * Returns id of NUMA node where current thread is running
   * Supports virtual and platform threads
   */
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

  /**
   * Get id of CPU where current thread is running
   * Supports virtual and platform threads
   */
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

  /**
   * Special function for Kunpeng-920
   */
  public static int getKunpengCCLId() {
    int cpuId = getCpuId();
    return cpuId / CCL_SIZE;
  }

  /**
   * Get carrier thread for virtual thread
   */
  public static Thread getCurrentCarrierThread() {

    try {
      return (Thread) GET_CARRIER_THREAD_METHOD_HANDLE.invoke();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get value from another thread's local variable
   *
   * @param tl     another thread's local variable
   * @param thread another thread
   */
  @SuppressWarnings("unchecked")
  public static <T> T getByThreadFromThreadLocal(ThreadLocal<T> tl, Thread thread) {
    try {
      return (T) GET_BY_THREAD_FROM_THREAD_LOCAL_METHOD_HANDLE.invoke(tl, thread);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set value to another thread's local variable
   *
   * @param tl     another thread's local variable
   * @param thread another thread
   */
  @SuppressWarnings("unchecked")
  public static <T> void setByThreadToThreadLocal(ThreadLocal<T> tl, Thread thread, Object obj) {
    try {
      SET_BY_THREAD_TO_THREAD_LOCAL_METHOD_HANDLE.invoke(tl, thread, obj);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}

