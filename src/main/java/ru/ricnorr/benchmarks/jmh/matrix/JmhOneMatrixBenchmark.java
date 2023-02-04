package ru.ricnorr.benchmarks.jmh.matrix;

import org.ejml.concurrency.EjmlConcurrency;
import org.ejml.simple.SimpleMatrix;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class JmhOneMatrixBenchmark {

    @Param("0")
    public int matrixSize;

    public SimpleMatrix matrixA;

    public SimpleMatrix matrixB;

    @Setup
    public void initMatrix() {
        // Don't use concurrency
        EjmlConcurrency.USE_CONCURRENT = false;

        var rand = new Random();
        matrixA = SimpleMatrix.random_DDRM(matrixSize, matrixSize, 0, Float.MAX_VALUE, rand);
        matrixB = SimpleMatrix.random_DDRM(matrixSize, matrixSize, 0, Float.MAX_VALUE, rand);
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void multMatrix(Blackhole bh) {
        bh.consume(matrixA.mult(matrixB));
    }
}
