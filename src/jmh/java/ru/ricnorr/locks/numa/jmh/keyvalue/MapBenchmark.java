package ru.ricnorr.locks.numa.jmh.keyvalue;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import ru.ricnorr.locks.numa.jmh.BenchmarkState;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static ru.ricnorr.locks.numa.jmh.BenchmarkUtil.initLock;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MapBenchmark {

    enum Action {
        PUT,
        GET,
        DELETE
    }

    public static void fillMap(Map<Integer, Integer> keyValueStorage) {
        Random randomGenerator = new Random();
        for (int i = 0; i < 50_000; i++) {
            keyValueStorage.put(randomGenerator.nextInt(0, 1_000_00), randomGenerator.nextInt(0, 1_000_00));
        }
    }

    /**
     * 0.1 chance to put
     * 0.1 chance to delete
     * 0.8 chance to get
     */
    public Action chooseAction() {
        int rand = ThreadLocalRandom.current().nextInt(0, 10);
        if (rand == 0) {
            return Action.PUT;
        }
        if (rand == 1) {
            return Action.DELETE;
        }
        return Action.GET;
    }

    public Integer executeAction(Action action, Map<Integer, Integer> keyValueStorage) {
        int key = ThreadLocalRandom.current().nextInt(0, 100_000);
        switch (action) {
            case PUT -> {
                int value = ThreadLocalRandom.current().nextInt(0, 100_000);
                return keyValueStorage.put(key, value);
            }
            case GET -> {
                return keyValueStorage.get(key);
            }
            case DELETE -> {
                return keyValueStorage.remove(key);
            }
            default -> throw new RuntimeException();
        }
    }

    @State(Scope.Benchmark) // All threads share this state
    public static class OrderedKeyValueState extends BenchmarkState {

        public Map<Integer, Integer> keyValueStorage = new TreeMap<>();

        public Lock lock;

        @Setup(Level.Trial)
        public void setUp() {
            lock = initLock(lockType);
            keyValueStorage.clear();
            fillMap(keyValueStorage);
        }
    }

    @State(Scope.Benchmark) // All threads share this state
    public static class HashKeyValueState extends BenchmarkState {

        public Map<Integer, Integer> keyValueStorage = new HashMap<>();

        public Lock lock;

        @Setup(Level.Trial)
        public void setUp() {
            lock = initLock(lockType);
            keyValueStorage.clear();
            fillMap(keyValueStorage);
        }
    }

    /**
     * Benchmark idea from the paper https://arxiv.org/pdf/1810.05600.pdf
     */
    @Benchmark
    @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public void ordered(Blackhole bh, OrderedKeyValueState state) {
        Action action = chooseAction();
        state.lock.lock();
        bh.consume(executeAction(action, state.keyValueStorage));
        state.lock.unlock();
    }

    /**
     * Benchmark idea from the paper https://dl.acm.org/doi/10.5555/3154690.3154748
     */
    @Benchmark
    @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public void hash(Blackhole bh, HashKeyValueState state) {
        Action action = chooseAction();
        state.lock.lock();
        bh.consume(executeAction(action, state.keyValueStorage));
        state.lock.unlock();
    }
}
