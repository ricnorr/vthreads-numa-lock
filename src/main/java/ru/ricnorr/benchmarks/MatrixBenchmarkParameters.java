package ru.ricnorr.benchmarks;

class MatrixBenchmarkParameters extends BenchmarkParameters {
    int beforeSize;
    int inSize;

    public MatrixBenchmarkParameters(int threads, LockType lockType, int beforeSize, int inSize) {
        super(threads, lockType);
        this.beforeSize = beforeSize;
        this.inSize = inSize;
    }

    @Override
    String getBenchmarkName() {
        return String.format(
            "Square matrix multiplication. Before crit.section size: %d. In crit.section size: %d",
            beforeSize,
            inSize
        );
    }
}
