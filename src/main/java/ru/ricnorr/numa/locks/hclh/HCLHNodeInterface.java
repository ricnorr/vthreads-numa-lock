package ru.ricnorr.numa.locks.hclh;

public interface HCLHNodeInterface {
    boolean waitForGrantOrClusterMaster(Integer myCluster);

    void prepareForLock(int clusterId);

    int getClusterID();

    boolean isSuccessorMustWait();

    void setSuccessorMustWait(boolean successorMustWait);

    boolean isTailWhenSpliced();

    void setTailWhenSpliced(boolean tailWhenSpliced);

}
