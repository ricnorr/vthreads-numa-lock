package io.github.ricnorr.benchmarks;

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
    CNA_Q,

    CNA_QSPIN,

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

    HMCS_Q,

    HMCS_QSPIN,

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

    VNA,

    VNA_2_Q,

    NUMA_MCS_RUN_ON_THIS_CARRIER_FEATURE_ENABLED,

    NUMA_MCS_YIELD_IF_DOESNT_CHANGED_NUMA,

    NUMA_MCS_YIELD_WHEN_SPIN_ON_GLOBAL,

    HSPIN_WITH_FAST,

    HSPIN_WITHOUT_FAST,

}
