package ru.ricnorr.benchmarks;

public enum LockType {
    UNFAIR_REENTRANT,

    FAIR_REENTRANT,

    MCS,

    TAS,

    TTAS,

    TICKET,

    CLH,

    /**
     * CNA
     */
    CNA_CCL,

    CNA_NUMA,

    CNA_CCL_PAD,
    CNA_NUMA_PAD,

    /**
     * HCLH
     */
    HCLH_CCL,

    HCLH_NUMA,

    HCLH_CCL_PAD,

    HCLH_NUMA_PAD,

    /**
     * HMCS
     */
    HMCS_CCL_NUMA,

    HMCS_CCL_NUMA_PAD,

    HMCS_CCL_NUMA_SUPERNUMA,

    HMCS_CCL_NUMA_SUPERNUMA_PAD,

    HMCS_CCL,

    HMCS_CCL_PAD,

    HMCS_NUMA,

    HMCS_NUMA_PAD,

    /**
     * Combo
     */
    TTAS_CCL_PLUS_CNA_NUMA,

    COMB_CNA_CCL_MCS_NUMA,

    COMB_HCLH_CCL_MCS_NUMA,
}
