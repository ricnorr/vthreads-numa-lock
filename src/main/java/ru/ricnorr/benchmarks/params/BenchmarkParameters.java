package ru.ricnorr.benchmarks.params;

import java.util.List;

import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.TimeValue;

public interface BenchmarkParameters {

  public static TimeValue BENCHMARK_MAX_DURATION = TimeValue.valueOf("1m");


  String getBenchmarkName();

  List<Options> getOptions();
}
