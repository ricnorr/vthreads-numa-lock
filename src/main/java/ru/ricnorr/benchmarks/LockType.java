package ru.ricnorr.benchmarks;

public enum LockType {
    REENTRANT,

    MCS,

    MCS_YIELD,

    TEST_SET,

    TEST_SET_YIELD,

    TEST_TEST_SET,

    TEST_TEST_SET_YIELD,

    TICKET,

    TICKET_YIELD,
}
