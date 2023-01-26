package ru.ricnorr.benchmarks;

public enum LockType {
    UNFAIR_REENTRANT,

    FAIR_REENTRANT,

    MCS,

    TEST_SET,

    TEST_TEST_SET,

    TICKET,

    HCLH,

    CLH,

    CNA,

    HCLH_CPU_CLUSTER_SPLIT,

    MCS_NO_PARK,
}
