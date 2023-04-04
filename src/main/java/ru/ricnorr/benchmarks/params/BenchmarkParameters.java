package ru.ricnorr.benchmarks.params;

import java.util.List;
import java.util.Map;

public interface BenchmarkParameters {
  String getBenchmarkName();

  List<Map<String, String>> getMap(LockParam lockParam);
}
