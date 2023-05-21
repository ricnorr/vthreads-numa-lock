package io.github.ricnorr.numa_locks;

/**
 * Interface for NUMA-aware locks for Virtual Threads
 *
 * @param <T> info for unlocking
 */
public interface VthreadNumaLock<T> {


  /**
   * Acquires the lock for virtual thread.
   *
   * <p>If the lock is not available then the current thread becomes
   * disabled for thread scheduling purposes and lies dormant until the
   * lock has been acquired.
   *
   * @return info for releasing lock
   */
  T lock();

  /**
   * Releases the lock
   *
   * @param unlockInfo object returned by {@link #lock()}
   */
  void unlock(T unlockInfo);

}
