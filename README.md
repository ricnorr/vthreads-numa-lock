[![Maven Central](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Fricnorr%2Fvthreads-numa-lock%2Fmaven-metadata.xml&versionPrefix=0.0.3)](https://central.sonatype.com/artifact/io.github.ricnorr/vthreads-numa-lock/0.0.3)
# vthreads-numa-lock.

Effective lock implementation for virtual threads on NUMA architecture in Java, which is called NUMA_MCS
Also my bachelor's diploma at ITMO university.
Also my research project.

## Goal of the project
Researching, implementing and benchmarking NUMA aware locks for light (virtual) threads in Java.
The target platform is [Kunpeng-920](https://www.hisilicon.com/en/products/Kunpeng/Huawei-Kunpeng/Huawei-Kunpeng-920) which has
ARM-architecture.

This work could not be done without help
of [@Aksenov239](https://github.com/Aksenov239), [@anton-malakhov](https://github.com/anton-malakhov), [@AndreyChurbanov](https://github.com/AndreyChurbanov).

## JDK patch (legacy, unsuccessful try to make results better)
https://github.com/ricnorr/jdk19/pull/3

## How to run benchmarks

### Install Gradle and Java
* Run ```./scripts/install_java.sh```
### Prepare conda,python,native libraries

* Run ```./scripts/activate-conda.sh``` from project's root to install miniconda
* Run ```./scripts/create-conda-envs.sh``` from project's root to install python libraries
* Run ```./scripts/build_libs.sh``` from project's root to compile native library
* 
### How to start benchmarks

* Run ```./scripts/run_bench.sh``` from the project's root
* Results are saved in ***results/benchmark_results.csv***
* Pictures saved in ***results/pictures***
* Pdf saved in ***results/result.pdf***

## Main ideas of the research
### Benchmark description
Benchmarks with label “48 cores” in title are measured on a system with HiSilicon Kunpeng-920, 48 cores, 2 NUMA nodes. Benchmarks with label “128 cores” in title are measured on a system with HiSillocon Kunpeng-920, 128 cores, 2 NUMA nodes.

Work is shared between threads, each thread in cycle does a work: multiply matrices (square matrices of size “out of c.s”), yields, acquires lock, multiple matrices (square matrix of size “in c.s”, which means in critical section), yields, releases lock. The throughput is a ratio of iterations number sum and total time.

### The main idea of the lock
NUMA_MCS consists of many MCS queues and boolean flag. Vthread tries to execute a CAS operation on a flag. If the operation is successful, the vthread becomes a lock owner, otherwise “slow path” happens. Slow path of lock acquiring consists of receiving NUMA id and joining the appropriate MCS queues. When the vthread is a leader of the local MCS queue, it waits for successful CAS on the flag. Scheme of lock acquiring is presented on the picture 1. 

![numa_mcs_lock drawio](https://github.com/ricnorr/vthreads-numa-lock/assets/31213770/a1a15b5c-0cb0-49ff-a55d-66faacedbdb0)

**Picture 1**. NUMA_MCS lock acquisition. VT1 from numa2 executes unsuccessful CAS and joins the local queue. 


When the vthread releases the lock, it sets the flag to false and unpark the next vthread in the local queue.  
The main problem of implementing effective locks for virtual thread on NUMA is the effect, when vthread is yielded inside critical section and resumed on another NUMA. Virtual threads migrate between NUMA nodes more often than platform threads. So NUMA_MCS expects that sometimes we are lucky and vthread enters and leaves critical section on the same NUMA. In the picture 2 presented benchmarks, comparing lock with local queue for each NUMA (NUMA_MCS)  and NUMA_MCS with just one queue (NUMA_MCS_ONE_QUEUE).

<img width="257" alt="image" src="https://github.com/ricnorr/vthreads-numa-lock/assets/31213770/f980f19f-fa0f-4cf7-9d50-5de1de6c8c9a">

**Picture 2**. Multiplication of matrices 50x50 inside the critical section, and doing nothing outside of the critical section. Very high contention.


### How to cache NUMA id for vthread
To get NUMA id you can perform syscall getcpu and cache the information. I suggest to cache the value on the carrier thread side and sometimes update information. 

### Use @Contended to solve false-sharing
This annotation is used to split data to different cache-lines and is a preferred way to solve the false-sharing problem. The cache line size can be set when initialising a VM.

### Comparing with standard locks
Below presented benchmarks comparing NUMA_MCS and ReentrantLock from the Java standard library. Results show that NUMA_MCS is more effective than ReentrantLock.

<img width="519" alt="image" src="https://github.com/ricnorr/vthreads-numa-lock/assets/31213770/4c5a0a5d-9d3c-4d98-a701-262f7d70c0b5">

<img width="515" alt="image" src="https://github.com/ricnorr/vthreads-numa-lock/assets/31213770/18fafddb-cde8-49cc-8e87-02976858c875">

<img width="517" alt="image" src="https://github.com/ricnorr/vthreads-numa-lock/assets/31213770/64cc88f9-638a-4818-ba10-922b362faf07">






