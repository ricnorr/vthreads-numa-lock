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

    HCLH_CCL_SPLIT,

    MCS_NO_PARK,

    HMCS,

    HCLH_CCL_SPLIT_BACKOFF,

    HMCS_PARK,

    HMCS_PARK_V2,

    HMCS_PARK_V3,

    HMCS_PARK_V4,

    HMCS_PARK_V5,

    HMCS_PARK_V6,
}
