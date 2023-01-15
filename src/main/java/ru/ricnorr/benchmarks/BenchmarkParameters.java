package ru.ricnorr.benchmarks;

import com.fasterxml.jackson.core.JsonParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.concurrent.locks.Lock;

public abstract class BenchmarkParameters {
    public int threads;
    public String lockSpec;

    public LockType lockType;

    public int actionsPerThread;

    public BenchmarkParameters(int threads, LockType lockType, int actionsPerThread, String lockSpec) {
        this.threads = threads;
        this.lockType = lockType;
        this.lockSpec = lockSpec;
        this.actionsPerThread = actionsPerThread;
    }

    public abstract String getBenchmarkName();
}
