# NUMA-aware-locks.

Repository for my bachelor's diploma at ITMO university

## Goal of the project
researching, implementing and benchmarking NUMA aware locks for light (virtual) threads in Java.
The target platform is [Kunpeng-920](https://www.hisilicon.com/en/products/Kunpeng/Huawei-Kunpeng/Huawei-Kunpeng-920) which has
ARM-architecture.

This work could not be done without help
of [@Aksenov239](https://github.com/Aksenov239), [@anton-malakhov](https://github.com/anton-malakhov), [@AndreyChurbanov](https://github.com/AndreyChurbanov).

## JDK patch
Locks use patched JDK https://github.com/ricnorr/jdk19/pull/3

## Current results:
Implemented a modification of HMCS lock, that works better than ReentrantLock on high contention, and equal on low contention.
![48 cores  Low contention _Throughput (op|ms)_all](https://user-images.githubusercontent.com/31213770/233806035-b67f1918-5c55-4daf-b90d-56813f1edd9c.png)
![48 cores  High contention _Throughput (op|ms)_all](https://user-images.githubusercontent.com/31213770/233806041-a511e0e8-276d-473d-b0e8-7e4b4600c59c.png)


## TODO plan:
- support Thread.yield() inside critical section

## Install tools

### Install Gradle and Custom JDK

* Run ```./scripts/install_java.sh```

### Prepare conda,python,native libraries

* Run ```activate-conda.sh``` to install miniconda
* Run ```create-conda-envs.sh``` to install python libraries
* Run ```./scripts/build_libs.sh``` from project root to compile native library

## How to start benchmarks

* ```run_bench.sh``` from the project's root
* Results are saved in ***results/benchmark_results.csv***
* Pictures saved in ***results/pictures***
* Pdf saved in ***results/result.pdf***


