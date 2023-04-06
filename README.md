# NUMA-aware-locks. 
Repository for my bachelor's diploma, researching, implementing and benchmarking NUMA aware locks for light (virtual) threads in Java.
Virtual threads are added in Java 19.  
The main goal of the project is to implement NUMA aware locks (effective on NUMA). The target platform is [Kunpeng-920](https://www.hisilicon.com/en/products/Kunpeng/Huawei-Kunpeng/Huawei-Kunpeng-920) which has ARM-architecture and NUMA.
This work could not be done without help of [@Aksenov239](https://github.com/Aksenov239), [@anton-malakhov](https://github.com/anton-malakhov), [@AndreyChurbanov](https://github.com/AndreyChurbanov).

## Current results:
### 128 cores system (4 NUMA nodes). High contention. Througput op/ms. Work inside critical section == work outside critical section
![Высокая конкуренция  Внешняя работа == внутренней _Пропускная способность (op|ms)_all](https://user-images.githubusercontent.com/31213770/230443993-eefb2c1f-9895-4797-93bc-d0bd62de4f88.png)
### 128 cores system (4 NUMA nodes). Low contention. Througput op/ms. Work outside critical section is in 128 times bigger than work inside critical section.
![Низкая конкуренция  Внешняя работа в 128 раз больше внутренней _Пропускная способность (op|ms)_all](https://user-images.githubusercontent.com/31213770/230444307-7c249a57-b183-4cb0-8439-90c7f5c2c578.png)


## Not implemented goals:
- measure latency of locks
- measure throughput of locks when there are thousands of light threads
- integration with the scheduler of light weight threads


# How to run
## Install instruments
* prepare java and gradle by ```install_java.sh``` 
* ```gradle wrapper``` in project dir
* prepare conda and python libraries
  * Run ```activate-conda.sh``` to install miniconda
  * Run ```create-conda-envs.sh``` 
* compile libraries: scripts/build_libs.sh 
## How to start benchmarks
* ```run_bench.sh``` from the project's root
* Results are saved in ***results/benchmark_results.csv***
* Pictures saved in ***results/pictures***
* Pdf saved in ***results/result.pdf***


