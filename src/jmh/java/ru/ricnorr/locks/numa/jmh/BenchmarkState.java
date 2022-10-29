package ru.ricnorr.locks.numa.jmh;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class BenchmarkState {
    @Param
    public LockType lockType;
}
