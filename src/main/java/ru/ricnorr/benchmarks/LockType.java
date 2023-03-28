package ru.ricnorr.benchmarks;

public enum LockType {

    SYNCHRONIZED,

    UNFAIR_REENTRANT,

    FAIR_REENTRANT,

    MCS,

    MCS_SLEEP,

    MCS_PARK,

    TAS,

    TTAS,

    TICKET,

    CLH,

    /**
     * CNA
     */
    CNA_CCL,

    CNA_NUMA,

    CNA_NUMA_SLEEP,

    CNA_NUMA_SLEEP_2,

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

    HMCS_NUMA_SUPERNUMA,

    HMCS_CCL_NUMA_V2,

    HMCS_NUMA_V3,

    HMCS_NUMA_V4,

    HMCS_NUMA_V5,

    HMCS_NUMA_V6,

    /**
     * HMCS NO PAD
     */
    HMCS_CCL_NUMA_UNPAD,
    HMCS_CCL_NUMA_SUPERNUMA_UNPAD,
    HMCS_CCL_UNPAD,
    HMCS_NUMA_UNPAD,

    /**
     * COMBO
     */
    COMB_CNA_CCL_MCS_NUMA,

    COMB_HCLH_CCL_MCS_NUMA,

    COMB_TTAS_CCL_HCLH_NUMA,

    COMB_TTAS_CCL_CNA_NUMA,

    COMB_TTAS_NUMA_MCS,

    TTAS_CCL_MCS,

}
