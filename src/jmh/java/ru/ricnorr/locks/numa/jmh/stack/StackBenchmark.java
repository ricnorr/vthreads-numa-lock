package ru.ricnorr.locks.numa.jmh.stack;

import org.openjdk.jmh.annotations.*;
import ru.ricnorr.locks.numa.jmh.BenchmarkState;
import ru.ricnorr.locks.numa.jmh.BenchmarkUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class StackBenchmark {

    enum Action {
        PUSH,
        POP,
    }

    public static void fillDeque(Deque<Integer> deque) {
        Random randomGenerator = new Random();
        for (int i = 0; i < 1_000_000; i++) {
            deque.push(randomGenerator.nextInt());
        }
    }

    /**
     * 0.5 chance to push
     * 0.5 chance to pop
     */
    public Action chooseAction() {
        int rand = ThreadLocalRandom.current().nextInt(0, 10);
        if (rand < 5) {
            return Action.PUSH;
        }
        return Action.POP;
    }

    @State(Scope.Benchmark) // All threads share this state
    public static class StackState extends BenchmarkState {

        public Deque<Integer> deque = new ArrayDeque<>();

        public Lock lock;

        @Setup(Level.Trial)
        public void setUp() {
            lock = BenchmarkUtil.initLock(lockType);
            deque.clear();
            fillDeque(deque);
        }
    }

    /**
     * Benchmark idea from the paper https://dl.acm.org/doi/10.5555/3154690.3154748
     */
    @Benchmark
    @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public void stack(
            StackState state
    ) {
        Action action = chooseAction();
        state.lock.lock();
        switch (action) {
            case PUSH -> state.deque.addFirst(ThreadLocalRandom.current().nextInt());
            case POP -> state.deque.pop();
            default -> throw new RuntimeException();
        }
        state.lock.unlock();
    }
}

