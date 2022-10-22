package ru.ricnorr.locks.numa.jmh.stack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import ru.ricnorr.locks.numa.jmh.BenchmarkUtil;
import ru.ricnorr.locks.numa.jmh.LockType;

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
    public static class DequeState {

        @Param
        public LockType lockType;

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
    public void stack(
        DequeState state
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

