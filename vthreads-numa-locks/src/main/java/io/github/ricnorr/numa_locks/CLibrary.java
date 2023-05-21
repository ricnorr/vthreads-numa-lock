package io.github.ricnorr.numa_locks;

import com.sun.jna.Library;
import com.sun.jna.Native;

interface CLibrary extends Library {
  CLibrary INSTANCE = Native.load("c", CLibrary.class);

  int syscall(int number, Object... args);
}