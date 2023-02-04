package ru.ricnorr.benchmarks;

public class MatrixBenchmarkParameters extends BenchmarkParameters {
    public int beforeSize;
    public int inSize;

    public double inMatrixMultTimeNanos;

    public double beforeMatrixMultTimeNanos;

    public MatrixBenchmarkParameters(int threads, LockType lockType, String lockSpec, int beforeSize, int inSize, int actionsPerThread,
                                     double beforeMatrixMultTimeNanos, double inMatrixMultTimeNanos) {
        super(threads, lockType, actionsPerThread, lockSpec);
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
