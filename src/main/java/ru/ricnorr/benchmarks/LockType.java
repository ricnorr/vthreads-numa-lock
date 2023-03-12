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

    /**
     * CNA NO PAD
     */
    CNA_CCL_PAD,
    CNA_NUMA_PAD,

    /**
     * HCLH
     */
    HCLH_CCL,

    HCLH_NUMA,

    /**
     * HCLH NO PAD
     */

    HCLH_CCL_NOPAD,

    HCLH_NUMA_NOPAD,


    /**
     * HMCS
     */
    HMCS_CCL_NUMA,
    HMCS_CCL_NUMA_SUPERNUMA,
    HMCS_CCL,
    HMCS_NUMA,

    /**
     * HMCS NO PAD
     */
    HMCS_CCL_NUMA_UNPAD,
    HMCS_CCL_NUMA_SUPERNUMA_UNPAD,
    HMCS_CCL_UNPAD,
    HMCS_NUMA_UNPAD,

    /**
     * Combo
     */
    TTAS_CCL_PLUS_CNA_NUMA,

    COMB_CNA_CCL_MCS_NUMA,

    COMB_HCLH_CCL_MCS_NUMA,
}
