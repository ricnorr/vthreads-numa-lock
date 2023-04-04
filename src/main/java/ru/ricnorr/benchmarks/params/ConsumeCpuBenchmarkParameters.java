package ru.ricnorr.benchmarks.params;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConsumeCpuBenchmarkParameters implements BenchmarkParameters {

  public long beforeCpuTokens;

  public long inCpuTokens;

  public Boolean yieldInCrit;

  public Integer yieldsBefore;

  public Integer threadsFrom;

  public List<Integer> threads;

  public int actionsCount;

  public int warmupIterations;

  public int measurementIterations;

  public int forks;

  public Map<String, String> profilerParams;

  public String title;

  public boolean skip;

  @Override
  public String getBenchmarkName() {
    return null;
  }

  @Override
  public List<Map<String, String>> getMap(LockParam lockParam) {
    assert (!lockParam.skip);
    if (threadsFrom != null) {
      threads = threads.stream().filter(it -> it >= threadsFrom).collect(Collectors.toList());
    }
    return threads.stream().map(it -> {
          var res = new HashMap<String, String>();
          res.put("lockType", lockParam.name.name());
          assert (title != null);
          res.put("beforeCpuTokens", Long.toString(beforeCpuTokens));
          res.put("inCpuTokens", Long.toString(inCpuTokens));
          res.put("threads", Long.toString(it));
          res.put("actionsCount", Long.toString(actionsCount));
          res.put("warmupIterations", Long.toString(warmupIterations));
          res.put("forks", Integer.toString(forks));
          res.put("measurementIterations", Integer.toString(measurementIterations));
          if (yieldInCrit != null) {
            res.put("yieldInCrit", Boolean.toString(yieldInCrit));
          }
          if (yieldsBefore != null) {
            res.put("yieldsBefore", Long.toString(yieldsBefore));
          }
          if (profilerParams != null) {
            res.putAll(profilerParams);
          }
          return res;
        }
    ).collect(Collectors.toList());
  }
}
