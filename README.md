# NUMA-aware-locks
Repository for my bachelor's diploma, implementing and benchmarking NUMA aware locks.
# Install instruments
* prepare java and gradle
* ```install_java.sh``` 
* ```gradle wrapper``` in project dir
* prepare conda and python libraries
  * Run ```activate-conda.sh``` to install miniconda
  * Run ```create-conda-envs.sh``` 
# How to run benchmarks
* ```run_bench.sh``` from the project's root
* Results are saved in ***results/benchmark_results.csv***
* Pictures saved in ***results/pictures***
* Pdf saved in ***results/result.pdf***
# How to run jcstress
* ```gradle jcstress``` to run all tests
* ```gradle jcstress --tests JcStressMCS|SomeAnotherStress``` to run specific tests

[comment]: <> (# How to enable perfnorm)

[comment]: <> (* ```sudo sysctl -w kernel.perf_event_paranoid=1```)
## Useful links
https://github.com/Valloric/jmh-playground
https://github.com/openjdk/jmh
https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples
https://github.com/deepu105/java-loom-benchmarks/blob/main/src/main/java/org/sample/LoomBenchmark.java
https://mail.openjdk.org/pipermail/jmh-dev/2015-May/001901.html
https://github.com/openjdk/jcstress  
https://www.javacodegeeks.com/2016/04/jlbh-introducing-java-latency-benchmarking-harness.html  
http://psy-lob-saw.blogspot.com/2013/05/using-jmh-to-benchmark-multi-threaded.html  
http://psy-lob-saw.blogspot.com/2013/12/jaq-spsc-latency-benchmarks1.html
https://hal.inria.fr/tel-01263203v2/document  
https://www.oracle.com/technical-resources/articles/java/architect-benchmarking.html  
https://shipilev.net/talks/jvmls-July2013-benchmarking.pdf  
https://www.baeldung.com/java-testing-multithreaded
## Отчет
### Запуск 23 января, arm, 128
https://drive.google.com/drive/folders/1ocNH0UDuKj5nHrHtIzSVInLM9tsOWJiW?usp=sharing




