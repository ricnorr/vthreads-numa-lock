package ru.ricnorr.benchmarks.params;

import java.util.List;

import org.openjdk.jmh.runner.options.Options;

public interface BenchmarkParameters {
  String getBenchmarkName();

  List<Options> getOptions();
}
