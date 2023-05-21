package io.github.ricnorr.benchmarks;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface Affinity extends Library {

  Affinity affinityLib = (Affinity) Native.loadLibrary("affinity", Affinity.class);

  int pinToCore(int cpuId);

}
