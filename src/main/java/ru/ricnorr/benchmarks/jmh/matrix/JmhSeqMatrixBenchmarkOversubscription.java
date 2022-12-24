package ru.ricnorr.benchmarks.jmh.matrix;

import org.ejml.concurrency.EjmlConcurrency;
import org.ejml.simple.SimpleMatrix;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.openjdk.jmh.annotations.Scope.Benchmark;

@State(Benchmark)
public class JmhSeqMatrixBenchmarkOversubscription {

    @Param("0")
    public int beforeSize;

    @Param("0")
    public int inSize;

    @Param("0")
    public int actionsPerThread;

    @Param("0")
    public int threads;

    SimpleMatrix beforeMatrixA;

    SimpleMatrix beforeMatrixB;

    SimpleMatrix inMatrixA;

    SimpleMatrix inMatrixB;

    @Setup
    public void init() {
        // Don't use concurrency
        EjmlConcurrency.USE_CONCURRENT = false;

        Random rand = new Random();
        beforeMatrixA = SimpleMatrix.random_DDRM(beforeSize, beforeSize, 0, Float.MAX_VALUE, rand);
        beforeMatrixB = SimpleMatrix.random_DDRM(beforeSize, beforeSize, 0, Float.MAX_VALUE, rand);

        inMatrixA = SimpleMatrix.random_DDRM(inSize, inSize, 0, Float.MAX_VALUE, rand);
        inMatrixB = SimpleMatrix.random_DDRM(inSize, inSize, 0, Float.MAX_VALUE, rand);

    }


    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void bench(Blackhole bh) {
        bh.consume(beforeMatrixA.mult(beforeMatrixB));
        for (int i = 0; i < actionsPerThread * threads; i++) {
            bh.consume(inMatrixA.mult(inMatrixB));
        }
    }

}
