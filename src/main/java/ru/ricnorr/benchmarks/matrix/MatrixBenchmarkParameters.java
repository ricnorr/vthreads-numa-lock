package ru.ricnorr.benchmarks.matrix;

import ru.ricnorr.benchmarks.BenchmarkParameters;
import ru.ricnorr.benchmarks.LockType;

public class MatrixBenchmarkParameters extends BenchmarkParameters {
    public int beforeSize;
    public int inSize;

    public double inMatrixMultTimeNanos;

    public double beforeMatrixMultTimeNanos;

    public MatrixBenchmarkParameters(int threads, LockType lockType, int beforeSize, int inSize, int actionsPerThread,
                                     double beforeMatrixMultTimeNanos, double inMatrixMultTimeNanos) {
        super(threads, lockType, actionsPerThread);
        this.beforeSize = beforeSize;
        this.inSize = inSize;
        this.beforeMatrixMultTimeNanos = beforeMatrixMultTimeNanos;
        this.inMatrixMultTimeNanos = inMatrixMultTimeNanos;
    }

    @Override
    public String getBenchmarkName() {
        return String.format(
            "Square matrix multiplication. Before crit.section size: %d. In crit.section size: %d",
            beforeSize,
            inSize
        );
    }
}
