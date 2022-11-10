package ru.ricnorr.locks.numa.jmh.matrix;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import ru.ricnorr.locks.numa.jmh.BenchmarkState;
import ru.ricnorr.locks.numa.jmh.BenchmarkUtil;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MatrixMultiplicationBenchmark {

    @State(Scope.Benchmark)
    public static class MatrixState extends BenchmarkState {

        @Param("1000")
        public int inSectionMatrixSize;

        @Param("1000")
        public int afterSectionMatrixSize;

        public Lock lock;

        public int[][] inSectionMatrix1;
        public int[][] inSectionMatrix2;

        public int[][] afterSectionMatrix1;
        public int[][] afterSectionMatrix2;

        @Setup(Level.Trial)
        public void setUp() {
            lock = BenchmarkUtil.initLock(lockType);
            inSectionMatrix1 = new int[inSectionMatrixSize][inSectionMatrixSize];
            inSectionMatrix2 = new int[inSectionMatrixSize][inSectionMatrixSize];
            afterSectionMatrix1 = new int[afterSectionMatrixSize][afterSectionMatrixSize];
            afterSectionMatrix2 = new int[afterSectionMatrixSize][afterSectionMatrixSize];
            fillMatrix(inSectionMatrix1, inSectionMatrixSize);
            fillMatrix(inSectionMatrix2, inSectionMatrixSize);
            fillMatrix(afterSectionMatrix1, afterSectionMatrixSize);
            fillMatrix(afterSectionMatrix2, afterSectionMatrixSize);
        }

        private void fillMatrix(int[][] matrix, int size) {
            Random r = new Random();
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    matrix[i][j] = r.nextInt();
                }
            }
        }
    }

    @Benchmark
    @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
    public int[][] matrixMultiplication(Blackhole bh, MatrixState state) {
        state.lock.lock();
        bh.consume(multiplyMatrixes(state.inSectionMatrix1, state.inSectionMatrix2));
        state.lock.unlock();
        return multiplyMatrixes(state.afterSectionMatrix1, state.afterSectionMatrix2);
    }

    private int[][] multiplyMatrixes(int[][] matrix1, int[][] matrix2) {
        for (int i = 0; i < matrix1.length; i++) {
            for (int j = 0; j < matrix2[0].length; j++) {
                for (int k = 0; k < matrix1[0].length; k++) {
                    matrix1[i][k] += matrix1[i][k] * matrix2[k][j];
                }
            }
        }
        return matrix1;
    }
}
