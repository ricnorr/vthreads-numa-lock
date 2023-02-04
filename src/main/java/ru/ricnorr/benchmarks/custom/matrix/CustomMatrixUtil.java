package ru.ricnorr.benchmarks.custom.matrix;

import org.ejml.simple.SimpleMatrix;
import ru.ricnorr.benchmarks.params.MatrixBenchmarkParameters;

import java.util.Random;
import java.util.concurrent.locks.Lock;

public class CustomMatrixUtil {
    public static Random rand = new Random();

    public static SimpleMatrix initMatrix(int size) {
        return SimpleMatrix.random_DDRM(size, size, 0, Float.MAX_VALUE, rand);
    }

    public static double estimateMatrixMultiplicationTimeNanos(int size) {
        long iterations = 1000;
        var matrix1 = initMatrix(size);
        var matrix2 = initMatrix(size);
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            matrix1.mult(matrix2);
        }
        long stop = System.nanoTime();

        return (stop - start) / (iterations * 1.0);
    }

    public static Runnable initMatrixWithoutLockRunnable(MatrixBenchmarkParameters matrixParam) {

        SimpleMatrix beforeMatrixA = initMatrix(matrixParam.beforeSize);
        SimpleMatrix beforeMatrixB = initMatrix(matrixParam.beforeSize);

        SimpleMatrix inMatrixA = initMatrix(matrixParam.inSize);
        SimpleMatrix inMatrixB = initMatrix(matrixParam.inSize);

        // iM * T > bM
        if (matrixParam.inMatrixMultTimeNanos * matrixParam.threads > matrixParam.beforeMatrixMultTimeNanos) {
            return () -> {
                beforeMatrixA.mult(beforeMatrixB);
                // bM + iM * T * I
                for (int i = 0; i < matrixParam.actionsPerThread * matrixParam.threads; i++) {
                    inMatrixA.mult(inMatrixB);
                }
            };
        } else {
            return () -> {
                // I * (bM + iM)
                for (int i = 0; i < matrixParam.actionsPerThread; i++) {
                    beforeMatrixA.mult(beforeMatrixB);
                    inMatrixA.mult(inMatrixB);
                }
            };
        }
    }

    public static Runnable initMatrixWithLockRunnable(Lock lock, MatrixBenchmarkParameters matrixParam) {
        SimpleMatrix beforeMatrixA = initMatrix(matrixParam.beforeSize);
        SimpleMatrix beforeMatrixB = initMatrix(matrixParam.beforeSize);

        SimpleMatrix inMatrixA = initMatrix(matrixParam.inSize);
        SimpleMatrix inMatrixB = initMatrix(matrixParam.inSize);

        return () -> {
            beforeMatrixA.mult(beforeMatrixB);
            lock.lock();
            inMatrixA.mult(inMatrixB);
            lock.unlock();
        };
    }
}
