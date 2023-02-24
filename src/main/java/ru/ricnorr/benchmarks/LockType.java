package ru.ricnorr.benchmarks;

public enum LockType {
    UNFAIR_REENTRANT,

    FAIR_REENTRANT,

    MCS,

    MCS_WITH_PADDING,

    TAS,

    TTAS,

    TICKET,

    CLH,

    /**
     * CNA
     */
    CNA_CCL_NO_PAD,

    CNA_NUMA_NO_PAD,

    CNA_CCL_PAD,
    CNA_NUMA_PAD,

    /**
     * HCLH
     */
    HCLH_CCL,

    HCLH_NUMA,

    HMCS_CCL_NUMA,

    HMCS_CCL_NUMA_PADDING,

    HMCS_CCL_NUMA_SUPERNUMA,

    HMCS_CCL_NUMA_SUPERNUMA_PADDING,

    HMCS_CCL,

    HMCS_CCL_PAD,

    HMCS_NUMA,

    HMCS_NUMA_PAD
}
