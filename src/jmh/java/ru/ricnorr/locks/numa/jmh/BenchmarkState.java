package ru.ricnorr.locks.numa.jmh;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class BenchmarkState {
    @Param({"REENTRANT", "MCS", "MCS_YIELD", "TEST_SET", "TEST_SET_YIELD", "TEST_TEST_SET", "TEST_TEST_SET_YIELD", "TICKET", "TICKET_YIELD"})
    public LockType lockType;
}
